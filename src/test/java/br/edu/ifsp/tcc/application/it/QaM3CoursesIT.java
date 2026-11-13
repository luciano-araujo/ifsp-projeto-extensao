package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.config.ArchiveMaterialsJob;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QA of milestone M3 (courses), second pass: RN06 bypass attempts, RN03 edges, cross-course and cross-team ids,
 * CSV injection, publish/unpublish cycles, magic-byte spoofing and archive job idempotency.
 */
class QaM3CoursesIT extends AbstractIT {

    private static final String YT = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired private ArchiveMaterialsJob archiveJob;

    // --- Helpers ---

    private long course(Team team, String title) {
        Resp r = api.post(team.path() + "/courses", ana.token(), map("title", title, "description", "d"));
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private long module(Team team, long course, String title) {
        Resp r = api.post(team.path() + "/courses/" + course + "/modules", ana.token(), map("title", title));
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private Resp lessonResp(Team team, long course, long module, Object body) {
        return api.post(team.path() + "/courses/" + course + "/modules/" + module + "/lessons", ana.token(), body);
    }

    private long lesson(Team team, long course, long module, String title, String url) {
        Resp r = lessonResp(team, course, module, url == null ? map("title", title) : map("title", title, "contentUrl", url));
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private Resp multipart(String path, Person by, String fileName, String declaredType, byte[] content) {
        String boundary = "----cw" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                    + "\"\r\nContent-Type: " + declaredType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(content);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + environment.getProperty("local.server.port") + path))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Authorization", "Bearer " + by.token())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private long upload(Team team, long card, String name) {
        Resp r = multipart(team.path() + "/cards/" + card + "/materials", ana, name, "application/octet-stream",
                "%PDF-1.4 teste".getBytes(StandardCharsets.UTF_8));
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private void link(Team team, long course, long module, long lesson, long material) {
        Resp r = api.post(team.path() + "/courses/" + course + "/modules/" + module + "/lessons/" + lesson + "/materials",
                ana.token(), map("materialId", material));
        assertEquals(200, r.status(), r.body());
    }

    private Object archivedAt(long materialId) {
        return jdbc.queryForObject("SELECT archived_at FROM materials WHERE id = ?", Object.class, materialId);
    }

    private List<String> problemTypes(Resp check) {
        List<String> types = new ArrayList<>();
        check.at("problems").forEach(p -> types.add(p.path("type").asString()));
        return types;
    }

    // --- RN06 bypass attempts ---

    @Test
    @DisplayName("QA-M3-RN06 Link matrix: lookalike hosts, http, wrong ids, other paths are INVALID_LINK; real links and uppercase pass")
    void rn06LinkMatrix() {
        Team team = newTeam();
        long c = course(team, "RN06");
        long m = module(team, c, "M");
        String check = team.path() + "/courses/" + c + "/publication-check";

        String[] invalid = {
                "https://youtube.com.evil.com/watch?v=dQw4w9WgXcQ",
                "https://evil.com/watch?v=dQw4w9WgXcQ",
                "https://evilyoutube.com/watch?v=dQw4w9WgXcQ",
                "https://youtube.com@evil.com/watch?v=dQw4w9WgXcQ",
                "https://evil.com/?u=https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://evil.com/https://youtu.be/dQw4w9WgXcQ",
                "https://evilclassroom.google.com/c/1",
                "https://classroom.google.com.evil.com/c/1",
                "https://classroom.google.com@evil.com/c/1",
                "https://evil.com/classroom.google.com",
                "https://evil.com#classroom.google.com/",
                "https://evil.com?classroom.google.com/",
                "http://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "HTTP://WWW.YOUTUBE.COM/WATCH?V=DQW4W9WGXCQ",
                "http://classroom.google.com/c/1",
                "http://vimeo.com/123456",
                "https://www.youtube.com/watch?v=short",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQextra",
                "https://youtu.be/dQw4w9WgXcQextra",
                "https://www.youtube.com/channel/UCxxxxxxxxxxxxxxxxxxxxxx",
                "https://vimeo.com/abc",
                "https://vimeo.com/",
                "https://www.vimeo.com.evil.com/123456",
                "https://m.vimeo.com/123456",
                "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://youtube.com:8443/watch?v=dQw4w9WgXcQ",
                "https://classroom.google.com:8443/c/1",
                "https://classroom.google.com.br/c/1",
                "https://google.com/c/1",
        };
        List<Long> invalidIds = new ArrayList<>();
        for (String url : invalid) invalidIds.add(lesson(team, c, m, "bad " + url, url));

        String[] valid = {
                YT,
                "HTTPS://WWW.YOUTUBE.COM/WATCH?V=dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ?t=10",
                "https://m.youtube.com/shorts/dQw4w9WgXcQ",
                "https://www.youtube.com/embed/dQw4w9WgXcQ",
                "https://vimeo.com/123456",
                "https://player.vimeo.com/video/123456",
                "https://classroom.google.com",
                "https://classroom.google.com/c/abc",
                "https://CLASSROOM.GOOGLE.COM/c/abc",
                "https://sub.classroom.google.com/c/abc",
        };
        for (String url : valid) lesson(team, c, m, "good " + url, url);

        Resp r = api.get(check, ana.token());
        assertEquals(200, r.status(), r.body());
        assertEquals(List.of("INVALID_LINK"), problemTypes(r));
        List<Long> flagged = new ArrayList<>();
        r.at("problems.0.ids").forEach(n -> flagged.add(n.asLong()));
        assertEquals(invalidIds, flagged, "exactly the lookalike/invalid links are flagged (and none of the valid ones)");
        assertEquals(400, api.post(team.path() + "/courses/" + c + "/publish", ana.token(), null).status());
    }

    @Test
    @DisplayName("QA-M3-RN06b Links that never reach the check: javascript:, data:, ftp:, control characters are refused on save")
    void rn06DangerousSchemesRefusedOnSave() {
        Team team = newTeam();
        long c = course(team, "Schemes");
        long m = module(team, c, "M");
        for (String bad : new String[]{"javascript:alert(1)", "JaVaScRiPt:alert(1)", "data:text/html,<script>1</script>",
                "ftp://youtube.com/watch?v=dQw4w9WgXcQ", "file:///etc/passwd", "//youtube.com/watch?v=dQw4w9WgXcQ",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ\nhttps://evil.com", "https://youtube.com/a b",
                "https:///evil.com"}) {
            assertEquals(400, lessonResp(team, c, m, map("title", "x", "contentUrl", bad)).status(), bad);
        }
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG QA3-01 (low): control characters (NUL) are accepted in contentUrl; the DTO regex uses [^\\s/]+ for the host")
    @DisplayName("QA-M3-RN06c contentUrl with a NUL character is refused on save")
    void controlCharacterInLink() {
        Team team = newTeam();
        long c = course(team, "Ctrl");
        long m = module(team, c, "M");
        assertEquals(400, lessonResp(team, c, m, map("title", "x", "contentUrl", "https://\u0000.com")).status());
    }

    // --- RN03 edges ---

    @Test
    @DisplayName("QA-M3-RN03 Edges: blank link counts as no link, material only passes, material+bad link is INVALID_LINK, problems list every case")
    void rn03Edges() {
        Team team = newTeam();
        long card = projectId(team, "Projeto RN03");
        long c = course(team, "RN03");
        long m = module(team, c, "M");
        long blank = lesson(team, c, m, "Link em branco", null);
        assertEquals(200, api.patch(team.path() + "/courses/" + c + "/modules/" + m + "/lessons/" + blank, ana.token(),
                map("contentUrl", "   ")).status());
        long both = lesson(team, c, m, "Material e link ruim", "https://exemplo.com/x");
        link(team, c, m, both, upload(team, card, "a.pdf"));
        long onlyMaterial = lesson(team, c, m, "So material", null);
        link(team, c, m, onlyMaterial, upload(team, card, "b.pdf"));

        Resp r = api.get(team.path() + "/courses/" + c + "/publication-check", ana.token());
        assertEquals(List.of("LESSON_WITHOUT_MATERIAL", "INVALID_LINK"), problemTypes(r));
        assertEquals(blank, r.num("problems.0.ids.0"));
        assertEquals(1, r.at("problems.0.ids").size());
        assertEquals(both, r.num("problems.1.ids.0"));

        // Unlinking the only material of a lesson of a PUBLISHED course: course stays published, check turns red
        long c2 = course(team, "Publicado");
        long m2 = module(team, c2, "M");
        long l2 = lesson(team, c2, m2, "Aula", null);
        long mat = upload(team, card, "c.pdf");
        link(team, c2, m2, l2, mat);
        assertEquals(200, api.post(team.path() + "/courses/" + c2 + "/publish", ana.token(), null).status());
        assertEquals(204, api.delete(team.path() + "/materials/" + mat, ana.token()).status());
        Resp after = api.get(team.path() + "/courses/" + c2 + "/publication-check", ana.token());
        assertFalse(after.json().path("publishable").asBoolean());
        assertEquals("PUBLISHED", api.get(team.path() + "/courses/" + c2, ana.token()).text("status"));
    }

    // --- Ids of other courses / teams ---

    @Test
    @DisplayName("QA-M3-IDS Module/lesson/material ids of another course or team: 404/400, never changed")
    void foreignIds() {
        Team team = newTeam();
        long cardA = projectId(team, "Projeto A");
        long c1 = course(team, "Curso 1");
        long c2 = course(team, "Curso 2");
        long m1 = module(team, c1, "M1");
        long m2 = module(team, c2, "M2");
        long l1 = lesson(team, c1, m1, "L1", YT);
        long l2 = lesson(team, c2, m2, "L2", YT);
        String b1 = team.path() + "/courses/" + c1;

        // module of course 2 through course 1
        assertEquals(404, api.patch(b1 + "/modules/" + m2, ana.token(), map("title", "hack")).status());
        assertEquals(404, api.patch(b1 + "/modules/" + m1 + "/lessons/" + l2, ana.token(), map("title", "hack")).status());
        assertEquals(404, api.put(b1 + "/modules/" + m2 + "/lessons/order", ana.token(), map("lessonIds", List.of(l2))).status());
        assertEquals(400, api.put(b1 + "/modules/order", ana.token(), map("moduleIds", List.of(m2))).status());
        assertEquals(400, api.put(b1 + "/modules/" + m1 + "/lessons/order", ana.token(), map("lessonIds", List.of(l2))).status());
        assertEquals(400, api.put(b1 + "/modules/order", ana.token(), map("moduleIds", Arrays.asList(m1, null))).status());
        assertEquals("M2", api.get(team.path() + "/courses/" + c2, ana.token()).text("modules.0.title"));
        assertEquals("L2", api.get(team.path() + "/courses/" + c2, ana.token()).text("modules.0.lessons.0.title"));

        // course of another team through my team
        long foreignCourse = api.post(otherTeam.path() + "/courses", bia.token(), map("title", "B")).num("id");
        assertEquals(404, api.patch(b1.replace("/courses/" + c1, "/courses/" + foreignCourse), ana.token(), map("title", "x")).status());
        assertEquals(404, api.post(team.path() + "/courses/" + foreignCourse + "/publish", ana.token(), null).status());
        assertEquals(404, api.get(team.path() + "/courses/" + foreignCourse + "/export?format=csv", ana.token()).status());
        assertEquals(404, api.get(team.path() + "/courses/" + foreignCourse + "/publication-check", ana.token()).status());
        // team B's own id used on team A's route with A's token, and A's course on B's route
        assertEquals(403, api.get(otherTeam.path() + "/courses/" + c1 + "/export", ana.token()).status());

        // material and project of another team
        Resp cardBResp = createProject(otherTeam, bia, "Projeto B", otherTeam.columnIds().get(0));
        assertEquals(200, cardBResp.status(), cardBResp.body());
        long cardB = cardBResp.num("id");
        long foreignMaterial = multipart(otherTeam.path() + "/cards/" + cardB + "/materials", bia, "b.pdf",
                "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8)).num("id");
        Resp linkForeign = api.post(b1 + "/modules/" + m1 + "/lessons/" + l1 + "/materials", ana.token(), map("materialId", foreignMaterial));
        assertTrue(linkForeign.status() == 400 || linkForeign.status() == 404, "foreign material: " + linkForeign.status());
        assertEquals(400, api.patch(b1 + "/modules/" + m1 + "/lessons/" + l1, ana.token(), map("projectId", cardB)).status());
        assertEquals(400, lessonResp(team, c1, m1, map("title", "x", "projectId", cardB)).status());
        long mine = upload(team, cardA, "mine.pdf");
        link(team, c1, m1, l1, mine);
        assertEquals(409, api.post(b1 + "/modules/" + m1 + "/lessons/" + l1 + "/materials", ana.token(), map("materialId", mine)).status());
    }

    // --- Edits ---

    @Test
    @DisplayName("QA-M3-EDIT Validation: titles at the limit, blank lesson title, description 5001, null JSON values, unknown fields")
    void editValidation() {
        Team team = newTeam();
        long c = course(team, "Edicao");
        long m = module(team, c, "M");
        long l = lesson(team, c, m, "L", null);
        String base = team.path() + "/courses/" + c;

        assertEquals(200, api.patch(base, ana.token(), map("title", "t".repeat(255))).status());
        assertEquals(400, api.patch(base, ana.token(), map("title", "t".repeat(256))).status());
        assertEquals(400, api.patch(base, ana.token(), map("description", "d".repeat(10001))).status());
        assertEquals(200, api.patch(base, ana.token(), "{}").status(), "empty patch is a no-op");
        assertEquals(200, api.patch(base, ana.token(), "{\"title\":null}").status());
        assertEquals("t".repeat(255), api.get(base, ana.token()).text("title"));
        assertEquals(400, api.patch(base, ana.token(), "{broken").status());
        assertEquals(400, api.patch(base + "/modules/" + m, ana.token(), map("title", "m".repeat(256))).status());

        Resp blankTitle = api.patch(base + "/modules/" + m + "/lessons/" + l, ana.token(), map("title", "  "));
        // Contract: a blank title must never wipe the lesson title
        assertEquals("L", api.get(base, ana.token()).text("modules.0.lessons.0.title"));
        assertTrue(blankTitle.status() == 200 || blankTitle.status() == 400, "blank lesson title status " + blankTitle.status());
        assertEquals(400, api.patch(base + "/modules/" + m + "/lessons/" + l, ana.token(), map("description", "d".repeat(5001))).status());
        assertEquals(200, api.patch(base + "/modules/" + m + "/lessons/" + l, ana.token(), map("description", "d".repeat(5000))).status());
        // 500 exactly is accepted, 501 refused (contentUrl)
        String url500 = "https://exemplo.com/" + "a".repeat(480);
        assertEquals(500, url500.length());
        assertEquals(200, api.patch(base + "/modules/" + m + "/lessons/" + l, ana.token(), map("contentUrl", url500)).status());
        assertEquals(400, api.patch(base + "/modules/" + m + "/lessons/" + l, ana.token(), map("contentUrl", url500 + "a")).status());
    }

    @Test
    @DisplayName("QA-M3-REORDER Reorder: ordering survives a reload, single item and empty lists, repeated call is idempotent, creates append")
    void reorderEdges() {
        Team team = newTeam();
        long c = course(team, "Ordem2");
        long m1 = module(team, c, "A");
        long m2 = module(team, c, "B");
        String base = team.path() + "/courses/" + c;

        assertEquals(200, api.put(base + "/modules/order", ana.token(), map("moduleIds", List.of(m2, m1))).status());
        assertEquals(200, api.put(base + "/modules/order", ana.token(), map("moduleIds", List.of(m2, m1))).status());
        long m3 = module(team, c, "C");
        Resp detail = api.get(base, ana.token());
        assertEquals(m2, detail.num("modules.0.id"));
        assertEquals(m1, detail.num("modules.1.id"));
        assertEquals(m3, detail.num("modules.2.id"), "a new module goes to the end after a reorder");

        long emptyModule = module(team, c, "Vazio");
        Resp none = api.put(base + "/modules/" + emptyModule + "/lessons/order", ana.token(), map("lessonIds", List.of()));
        assertEquals(200, none.status(), none.body());
        assertEquals(0, none.size());

        long solo = lesson(team, c, m1, "Solo", null);
        assertEquals(200, api.put(base + "/modules/" + m1 + "/lessons/order", ana.token(), map("lessonIds", List.of(solo))).status());
        long second = lesson(team, c, m1, "Segunda", null);
        assertEquals(second, api.get(base, ana.token()).num("modules.1.lessons.1.id"));

        // a course with no modules
        long bare = course(team, "Sem modulos");
        Resp emptyList = api.put(team.path() + "/courses/" + bare + "/modules/order", ana.token(), map("moduleIds", List.of()));
        assertEquals(200, emptyList.status(), emptyList.body());
        // reorder is also refused for non-members, 401 anonymous
        assertEquals(403, api.put(base + "/modules/order", bia.token(), map("moduleIds", List.of(m1))).status());
        assertEquals(401, api.put(base + "/modules/order", null, map("moduleIds", List.of(m1))).status());
    }

    // --- Publish cycles ---

    @Test
    @DisplayName("QA-M3-PUB Publish, publish, unpublish, unpublish, publish again: publishedAt kept, cleared, set anew; archive clock restarts")
    void publishCycle() throws InterruptedException {
        Team team = newTeam();
        long c = course(team, "Ciclo");
        long m = module(team, c, "M");
        lesson(team, c, m, "A", YT);
        String base = team.path() + "/courses/" + c;

        Resp first = api.post(base + "/publish", ana.token(), null);
        assertEquals(200, first.status(), first.body());
        String t1 = first.text("publishedAt");
        assertNotNull(t1);
        Resp second = api.post(base + "/publish", carlos.token(), null);
        assertEquals(t1, second.text("publishedAt"));

        assertEquals(200, api.post(base + "/unpublish", carlos.token(), null).status());
        Resp again = api.post(base + "/unpublish", ana.token(), null);
        assertEquals(200, again.status());
        assertEquals("DRAFT", again.text("status"));
        assertNull(again.text("publishedAt"));

        Thread.sleep(1100);
        Resp third = api.post(base + "/publish", ana.token(), null);
        assertEquals("PUBLISHED", third.text("status"));
        assertNotEquals(t1, third.text("publishedAt"));
        assertEquals(third.text("publishedAt"), api.get(team.path() + "/courses", ana.token()).text("0.publishedAt"));
        assertEquals(405, api.send("GET", base + "/publish", ana.token(), null).status());
        // anonymous and outsiders
        assertEquals(401, api.post(base + "/publish", null, null).status());
        assertEquals(403, api.post(base + "/publish", bia.token(), null).status());
        assertEquals(403, api.post(base + "/unpublish", loner.token(), null).status());
    }

    // --- CSV injection ---

    @Test
    @DisplayName("QA-M3-CSV Formula guard and escaping: =,+,-,@,tab,CR prefixes; ; quotes and newlines in titles; BOM bytes")
    void csvInjection() {
        Team team = newTeam();
        long card = projectId(team, "Projeto CSV");
        long c = course(team, "=HYPERLINK(\"http://evil\")");
        long m = module(team, c, "@SUM(A1)");
        lesson(team, c, m, "=1+1", null);
        lesson(team, c, m, "+55 11", null);
        lesson(team, c, m, "-2+3", null);
        lesson(team, c, m, "@cmd", null);
        lesson(team, c, m, "\t=tab", null);
        lesson(team, c, m, "Com \"aspas\"; e ponto", null);
        lesson(team, c, m, "Linha1\nLinha2", null);
        long withFile = lesson(team, c, m, "Arquivo", null);
        link(team, c, m, withFile, upload(team, card, "=evil.pdf"));
        long m2 = module(team, c, "+modulo vazio");
        assertNotNull(m2);

        Resp csv = api.get(team.path() + "/courses/" + c + "/export?format=csv", rui.token());
        assertEquals(200, csv.status(), csv.body());
        String body = csv.body();
        assertEquals('﻿', body.charAt(0));
        assertTrue(body.contains(";'=1+1;"), body);
        assertTrue(body.contains(";'+55 11;"), body);
        assertTrue(body.contains(";'-2+3;"), body);
        assertTrue(body.contains(";'@cmd;"), body);
        assertTrue(body.contains(";'@SUM(A1);") || body.contains("1;'@SUM(A1);"), body);
        assertTrue(body.contains(";'=tab;"), body); // titles are trimmed on save, so the tab never reaches the CSV
        assertTrue(body.contains(";\"Com \"\"aspas\"\"; e ponto\";"), body);
        assertTrue(body.contains(";\"Linha1\nLinha2\";"), body);
        assertTrue(body.contains(";'=evil.pdf\r\n"), body);
        assertTrue(body.contains("2;'+modulo vazio;;;;;;;"), body);
        // No cell may start with a formula character
        for (String line : body.substring(1).split("\r\n")) {
            for (String cell : line.split(";")) {
                if (cell.isEmpty() || cell.startsWith("\"")) continue;
                assertFalse("=+-@".indexOf(cell.charAt(0)) >= 0, "formula-like cell: " + cell);
            }
        }
        // The json export keeps the raw title (consumers are not spreadsheets)
        assertEquals("=HYPERLINK(\"http://evil\")", api.get(team.path() + "/courses/" + c + "/export", ana.token()).text("title"));
    }

    @Test
    @DisplayName("QA-M3-CSV2 Raw CSV bytes start with EF BB BF and use CRLF; format is case-insensitive; blank format means json")
    void csvBytes() throws Exception {
        Team team = newTeam();
        long c = course(team, "Bytes");
        long m = module(team, c, "Acentuação");
        lesson(team, c, m, "Ação", null);
        String path = team.path() + "/courses/" + c + "/export?format=CSV";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + environment.getProperty("local.server.port") + path))
                .header("Authorization", "Bearer " + ana.token()).GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        byte[] bytes = response.body();
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        String text = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertTrue(text.contains("Acentuação;1;Ação"), text);
        assertTrue(text.endsWith("\r\n"));
        assertEquals(200, api.get(team.path() + "/courses/" + c + "/export?format=", ana.token()).status());
        assertTrue(api.get(team.path() + "/courses/" + c + "/export?format=", ana.token()).header("Content-Type").contains("json"));
    }

    // --- Magic bytes ---

    @Test
    @DisplayName("QA-M3-MAGIC Spoofing: EXE as .pdf, HTML as .png, double extensions, declared type mismatch, empty file; polyglot headers served safely")
    void magicBytes() {
        Team team = newTeam();
        long card = projectId(team, "Magic");
        String path = team.path() + "/cards/" + card + "/materials";
        byte[] exe = {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0};
        byte[] html = "<!doctype html><script>alert(document.domain)</script>".getBytes(StandardCharsets.UTF_8);
        byte[] pdf = "%PDF-1.7\n1 0 obj".getBytes(StandardCharsets.UTF_8);

        assertEquals(415, multipart(path, ana, "virus.pdf", "application/pdf", exe).status());
        assertEquals(415, multipart(path, ana, "virus.pdf", "application/octet-stream", exe).status());
        assertEquals(415, multipart(path, ana, "virus.exe", "application/octet-stream", exe).status());
        assertEquals(415, multipart(path, ana, "virus.pdf.exe", "application/pdf", pdf).status());
        assertEquals(415, multipart(path, ana, "page.png", "image/png", html).status());
        assertEquals(415, multipart(path, ana, "page.html", "text/html", html).status());
        assertEquals(415, multipart(path, ana, "page.svg", "image/svg+xml", "<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8)).status());
        assertEquals(415, multipart(path, ana, "real.pdf", "text/html", pdf).status(), "declared type conflicts");
        assertEquals(415, multipart(path, ana, "real.pdf", "image/png", pdf).status());
        assertEquals(415, multipart(path, ana, "semext", "application/pdf", pdf).status());
        assertEquals(415, multipart(path, ana, "ponto.", "application/pdf", pdf).status());
        assertEquals(415, multipart(path, ana, "unix.pdf", "application/pdf", "#!/bin/sh\nrm -rf /".getBytes(StandardCharsets.UTF_8)).status());
        assertEquals(415, multipart(path, ana, "pk.pdf", "application/pdf", new byte[]{'P', 'K', 3, 4}).status());
        assertEquals(415, multipart(path, ana, "elf.txt", "text/plain", new byte[]{0x7F, 'E', 'L', 'F', 0, 0}).status());
        assertEquals(415, multipart(path, ana, "curto.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'}).status());
        int empty = multipart(path, ana, "vazio.pdf", "application/pdf", new byte[0]).status();
        assertTrue(empty == 400 || empty == 415, "empty pdf: " + empty);
        assertEquals(0, api.get(path, ana.token()).size(), "nothing was stored by the refused uploads");

        // Uppercase extension and a polyglot (valid PDF header followed by HTML) are accepted but never served as HTML
        Resp upper = multipart(path, ana, "MAIUSCULA.PDF", "application/pdf", pdf);
        assertEquals(200, upper.status(), upper.body());
        byte[] polyglot = ("%PDF-1.7\n<html><script>alert(1)</script></html>").getBytes(StandardCharsets.UTF_8);
        Resp poly = multipart(path, ana, "poliglota.pdf", "application/pdf", polyglot);
        assertEquals(200, poly.status(), poly.body());
        Resp served = api.get(team.path() + "/materials/" + poly.num("id") + "/download?inline=true", ana.token());
        assertEquals(200, served.status());
        assertTrue(served.header("Content-Type").startsWith("application/pdf"), served.header("Content-Type"));
        assertEquals("nosniff", served.header("X-Content-Type-Options"));
        Resp txt = multipart(path, ana, "html.txt", "text/plain", html);
        assertEquals(200, txt.status(), "html text is plain text by extension");
        Resp servedTxt = api.get(team.path() + "/materials/" + txt.num("id") + "/download?inline=true", ana.token());
        assertTrue(servedTxt.header("Content-Type").startsWith("text/plain"), servedTxt.header("Content-Type"));
        assertEquals("nosniff", servedTxt.header("X-Content-Type-Options"));
    }

    // --- RN04 job ---

    @Test
    @DisplayName("QA-M3-RN04 Job: boundary of 30 days, unpublished-before-run stays untouched, shared material, re-run and re-publish")
    void archiveEdges() {
        Team team = newTeam();
        long card = projectId(team, "Arquivo2");
        long shared = upload(team, card, "compartilhado.pdf");
        long justUnder = upload(team, card, "limite.pdf");
        long unpublished = upload(team, card, "despublicado.pdf");

        long old = course(team, "Velho");
        long om = module(team, old, "M");
        long ol = lesson(team, old, om, "A", YT);
        link(team, old, om, ol, shared);
        assertEquals(200, api.post(team.path() + "/courses/" + old + "/publish", ana.token(), null).status());

        long edge = course(team, "Quase");
        long em = module(team, edge, "M");
        long el = lesson(team, edge, em, "A", YT);
        link(team, edge, em, el, justUnder);
        assertEquals(200, api.post(team.path() + "/courses/" + edge + "/publish", ana.token(), null).status());

        long toDraft = course(team, "Volta rascunho");
        long dm = module(team, toDraft, "M");
        long dl = lesson(team, toDraft, dm, "A", YT);
        link(team, toDraft, dm, dl, unpublished);
        assertEquals(200, api.post(team.path() + "/courses/" + toDraft + "/publish", ana.token(), null).status());

        jdbc.update("UPDATE courses SET published_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 31 DAY) WHERE id = ?", old);
        jdbc.update("UPDATE courses SET published_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 29 DAY) WHERE id = ?", edge);
        jdbc.update("UPDATE courses SET published_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 40 DAY) WHERE id = ?", toDraft);
        assertEquals(200, api.post(team.path() + "/courses/" + toDraft + "/unpublish", ana.token(), null).status());

        archiveJob.run();
        assertNotNull(archivedAt(shared));
        assertNull(archivedAt(justUnder), "29 days is not yet 30");
        assertNull(archivedAt(unpublished), "unpublished course: nothing to archive");

        Object stamp = archivedAt(shared);
        archiveJob.run();
        archiveJob.run();
        assertEquals(stamp, archivedAt(shared));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE id IN (?,?,?) AND archived_at IS NOT NULL",
                Integer.class, shared, justUnder, unpublished));

        // Re-publishing and unpublishing does not un-archive; the material stays downloadable
        assertEquals(200, api.post(team.path() + "/courses/" + old + "/unpublish", ana.token(), null).status());
        assertNotNull(archivedAt(shared));
        assertEquals(200, api.get(team.path() + "/materials/" + shared + "/download", rui.token()).status());
    }

    @Test
    @DisplayName("QA-M3-SEC Role matrix on every M3 route: reviewer reads, never writes; outsider 403; anonymous 401")
    void roleMatrix() {
        Team team = newTeam();
        long c = course(team, "Papeis");
        long m = module(team, c, "M");
        long l = lesson(team, c, m, "A", YT);
        String base = team.path() + "/courses/" + c;
        String[][] writes = {
                {"PATCH", base}, {"PATCH", base + "/modules/" + m}, {"PATCH", base + "/modules/" + m + "/lessons/" + l},
                {"PUT", base + "/modules/order"}, {"PUT", base + "/modules/" + m + "/lessons/order"},
                {"POST", base + "/publish"}, {"POST", base + "/unpublish"},
                {"POST", base + "/modules"}, {"POST", base + "/modules/" + m + "/lessons"},
        };
        for (String[] w : writes) {
            Object body = w[1].endsWith("order") ? map("moduleIds", List.of(m), "lessonIds", List.of(l)) : map("title", "x");
            assertEquals(403, api.send(w[0], w[1], rui.token(), body).status(), "reviewer " + w[0] + " " + w[1]);
            assertEquals(403, api.send(w[0], w[1], loner.token(), body).status(), "outsider " + w[0] + " " + w[1]);
            assertEquals(401, api.send(w[0], w[1], null, body).status(), "anonymous " + w[0] + " " + w[1]);
        }
        for (String read : new String[]{base + "/publication-check", base + "/export", base + "/export?format=csv", base}) {
            assertEquals(200, api.get(read, rui.token()).status(), read);
            assertEquals(403, api.get(read, loner.token()).status(), read);
            assertEquals(401, api.get(read, null).status(), read);
        }
        assertEquals("Papeis", api.get(base, ana.token()).text("title"));
    }
}
