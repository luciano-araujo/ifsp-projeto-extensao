package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QA of milestone M2 (materials, reviews, summary): security and role-bypass probes that complement
 * MaterialsIT, CardReviewsIT and CardSummaryIT. Plan cases: SEC (cross-team, role bypass, traversal, header
 * injection, malformed multipart), UT-44, UT-52, REG.
 */
class QaM2SecurityIT extends AbstractIT {

    private static final byte[] PDF = "%PDF-1.4 qa".getBytes(StandardCharsets.UTF_8);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    // --- Helpers ---

    private String baseUrl() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    /** Sends raw bytes with the given content type (multipart bodies built by hand, boundaries under control). */
    private Resp raw(String method, String path, String token, String contentType, byte[] body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(60))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));
        if (contentType != null) request.header("Content-Type", contentType);
        if (token != null) request.header("Authorization", "Bearer " + token);
        try {
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Resp upload(Team team, long cardId, Person by, String fileName, byte[] content) {
        String boundary = "qa" + UUID.randomUUID().toString().replace("-", "");
        byte[] head = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                + "\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[head.length + content.length + tail.length];
        System.arraycopy(head, 0, body, 0, head.length);
        System.arraycopy(content, 0, body, head.length, content.length);
        System.arraycopy(tail, 0, body, head.length + content.length, tail.length);
        return raw("POST", team.path() + "/cards/" + cardId + "/materials", by.token(),
                "multipart/form-data; boundary=" + boundary, body);
    }

    private long uploadOk(Team team, long cardId, Person by, String fileName) {
        Resp r = upload(team, cardId, by, fileName, PDF);
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private record Lesson(long courseId, long moduleId, long lessonId) {
        String path(Team team) {
            return team.path() + "/courses/" + courseId + "/modules/" + moduleId + "/lessons/" + lessonId;
        }
    }

    private Lesson lessonOf(Team team, Person admin) {
        String base = team.path() + "/courses";
        long course = api.post(base, admin.token(), map("title", "Curso QA", "description", "d")).num("id");
        long module = api.post(base + "/" + course + "/modules", admin.token(), map("title", "Modulo")).num("id");
        long lesson = api.post(base + "/" + course + "/modules/" + module + "/lessons", admin.token(),
                map("title", "Aula")).num("id");
        return new Lesson(course, module, lesson);
    }

    // --- Role bypass ---

    @Test
    @DisplayName("SEC-M2-01 REVIEWER cannot write: upload, delete, link, unlink, summary, script, card edit/delete (UT-52)")
    void reviewerCannotWrite() {
        Team team = newTeam();
        long card = projectId(team, "Projeto do revisor");
        long material = uploadOk(team, card, ana, "a.pdf");
        Lesson lesson = lessonOf(team, ana);
        String words = String.join(" ", Collections.nCopies(120, "palavra"));
        assertEquals(200, api.put("/api/v1/kanban/" + card + "/script", ana.token(), map("content", "<p>" + words + "</p>")).status());

        assertEquals(403, upload(team, card, rui, "r.pdf", PDF).status());
        assertEquals(403, api.delete(team.path() + "/materials/" + material, rui.token()).status());
        assertEquals(403, api.post(lesson.path(team) + "/materials", rui.token(), map("materialId", material)).status());
        assertEquals(403, api.delete(lesson.path(team) + "/materials/" + material, rui.token()).status());
        assertEquals(403, api.post(team.path() + "/cards/" + card + "/summary", rui.token(), null).status());
        assertEquals(403, api.put("/api/v1/kanban/" + card + "/script", rui.token(), map("content", "<p>hack</p>")).status());
        assertEquals(403, api.put("/api/v1/kanban/" + card, rui.token(), map("title", "Titulo alterado")).status());
        assertEquals(403, api.delete(team.path() + "/cards/" + card, rui.token()).status());
        assertEquals(403, api.patch(team.path() + "/cards/" + card + "/move", rui.token(),
                map("columnId", team.columnIds().get(1))).status());

        // nothing changed
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE id = ?", Integer.class, material));
        assertEquals("Projeto do revisor", jdbc.queryForObject("SELECT title FROM kanban_items WHERE id = ?", String.class, card));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM card_summaries WHERE kanban_item_id = ?", Integer.class, card));
        // but the reviewer reads everything
        assertEquals(200, api.get(team.path() + "/cards/" + card + "/materials", rui.token()).status());
        assertEquals(200, api.get(team.path() + "/materials/" + material + "/download", rui.token()).status());
        assertEquals(200, api.get("/api/v1/kanban/" + card + "/script", rui.token()).status());
    }

    @Test
    @DisplayName("SEC-M2-02 CREATOR cannot review; outsider cannot read or write any M2 route")
    void creatorAndOutsiderLimits() {
        Team team = newTeam();
        long card = projectId(team, "Projeto limites");
        long material = uploadOk(team, card, carlos, "c.pdf");
        String reviews = team.path() + "/cards/" + card + "/reviews";

        assertEquals(403, api.post(reviews, carlos.token(), map("decision", "APPROVED")).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM card_reviews WHERE kanban_item_id = ?", Integer.class, card));

        for (String path : List.of(team.path() + "/cards/" + card + "/materials", team.path() + "/materials/" + material + "/download",
                reviews, team.path() + "/cards/" + card + "/summary")) {
            assertEquals(403, api.get(path, bia.token()).status(), "GET " + path);
            assertEquals(401, api.get(path, null).status(), "anonymous GET " + path);
            assertEquals(403, api.get(path, loner.token()).status(), "no-team GET " + path);
        }
        assertEquals(403, api.post(reviews, bia.token(), map("decision", "APPROVED")).status());
        assertEquals(403, api.delete(team.path() + "/materials/" + material, bia.token()).status());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE id = ?", Integer.class, material));
    }

    // --- Cross-team ---

    @Test
    @DisplayName("SEC-M2-03 Cross-team ids: other team's card, material, course, module and lesson are never reachable through my team")
    void crossTeamIds() {
        Team mine = newTeam();
        Team theirs = otherTeam;
        long theirCard = api.post(theirs.path() + "/cards", bia.token(), map("title", "Projeto de outra equipe",
                "description", "d", "targetAudience", "Estudantes iniciantes", "pedagogicalObjective", "Aprender o basico do tema",
                "columnId", theirs.columnIds().get(0))).num("id");
        long theirMaterial = uploadOk(theirs, theirCard, bia, "dela.pdf");
        Lesson theirLesson = lessonOf(theirs, bia);
        long myCard = projectId(mine, "Meu projeto");
        long myMaterial = uploadOk(mine, myCard, ana, "meu.pdf");
        Lesson myLesson = lessonOf(mine, ana);

        // through my team's path (I am ADMIN there)
        assertEquals(404, api.get(mine.path() + "/cards/" + theirCard + "/materials", ana.token()).status());
        assertEquals(404, upload(mine, theirCard, ana, "x.pdf", PDF).status());
        assertEquals(404, api.get(mine.path() + "/materials/" + theirMaterial + "/download", ana.token()).status());
        assertEquals(404, api.delete(mine.path() + "/materials/" + theirMaterial, ana.token()).status());
        assertEquals(404, api.get(mine.path() + "/cards/" + theirCard + "/reviews", ana.token()).status());
        assertEquals(404, api.post(mine.path() + "/cards/" + theirCard + "/reviews", ana.token(), map("decision", "APPROVED")).status());
        assertEquals(404, api.get(mine.path() + "/cards/" + theirCard + "/summary", ana.token()).status());
        assertEquals(404, api.post(mine.path() + "/cards/" + theirCard + "/summary", ana.token(), null).status());

        // linking: foreign material is a 400, foreign course/module/lesson ids are 404
        assertEquals(400, api.post(myLesson.path(mine) + "/materials", ana.token(), map("materialId", theirMaterial)).status());
        assertEquals(404, api.post(theirs.path() + "/courses/" + myLesson.courseId() + "/modules/" + myLesson.moduleId()
                + "/lessons/" + myLesson.lessonId() + "/materials", bia.token(), map("materialId", theirMaterial)).status());
        assertEquals(404, api.post(mine.path() + "/courses/" + theirLesson.courseId() + "/modules/" + theirLesson.moduleId()
                + "/lessons/" + theirLesson.lessonId() + "/materials", ana.token(), map("materialId", myMaterial)).status());
        assertEquals(404, api.delete(mine.path() + "/courses/" + theirLesson.courseId() + "/modules/" + theirLesson.moduleId()
                + "/lessons/" + theirLesson.lessonId() + "/materials/" + theirMaterial, ana.token()).status());
        // module of my course combined with a lesson of another module
        assertEquals(404, api.post(mine.path() + "/courses/" + myLesson.courseId() + "/modules/" + myLesson.moduleId()
                + "/lessons/" + theirLesson.lessonId() + "/materials", ana.token(), map("materialId", myMaterial)).status());

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE id = ?", Integer.class, theirMaterial));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM course_lesson_materials WHERE material_id IN (?, ?)", Integer.class, theirMaterial, myMaterial));
    }

    // --- Path traversal and malformed ids ---

    @Test
    @DisplayName("SEC-M2-04 Path traversal and malformed ids never give 200 or 500")
    void traversalIds() {
        Team team = newTeam();
        long card = projectId(team, "Projeto traversal");
        long material = uploadOk(team, card, ana, "t.pdf");
        String[] bad = {"..%2F..%2Fetc%2Fpasswd", "..", "%2e%2e", "abc", "-1", "0", "99999999999999999999", "1%3B2", "1%00", material + "%2F..%2F" + material,
                "1+OR+1%3D1"};
        for (String id : bad) {
            for (String path : List.of(team.path() + "/materials/" + id + "/download", team.path() + "/cards/" + id + "/materials",
                    team.path() + "/cards/" + id + "/reviews", team.path() + "/cards/" + id + "/summary")) {
                int status = raw("GET", path, ana.token(), null, null).status();
                assertTrue(status == 400 || status == 404, "GET " + path + " gave " + status);
            }
            int deleted = raw("DELETE", team.path() + "/materials/" + id, ana.token(), null, null).status();
            assertTrue(deleted == 400 || deleted == 404, "DELETE materials/" + id + " gave " + deleted);
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE id = ?", Integer.class, material));
    }

    // --- Header injection and file names ---

    @Test
    @DisplayName("SEC-M2-05 Hostile file names: no CR/LF in Content-Disposition, no path, no bidi, bounded length")
    void hostileFileNames() {
        Team team = newTeam();
        long card = projectId(team, "Projeto nomes");

        long encodedCrlf = uploadOk(team, card, ana, "evil%0d%0aSet-Cookie: a=b.pdf");
        long bidi = uploadOk(team, card, ana, "doc‮fdp.exe.pdf");
        long traversal = uploadOk(team, card, ana, "..\\..\\windows\\evil.pdf");
        long quote = uploadOk(team, card, ana, "o'nome com espaco.pdf");
        long accents = uploadOk(team, card, ana, "relatório ação ✓.pdf");

        for (long id : new long[]{encodedCrlf, bidi, traversal, quote, accents}) {
            for (String suffix : new String[]{"", "?inline=true"}) {
                Resp r = raw("GET", team.path() + "/materials/" + id + "/download" + suffix, ana.token(), null, null);
                assertEquals(200, r.status());
                String disposition = r.header("Content-Disposition");
                assertNotNull(disposition);
                assertFalse(disposition.contains("\r") || disposition.contains("\n"), disposition);
                assertTrue(disposition.contains("filename*=UTF-8''"), disposition);
                assertEquals("nosniff", r.header("X-Content-Type-Options"));
                assertTrue(r.header("Content-Security-Policy").contains("sandbox"));
                assertNull(r.header("Set-Cookie"), "an uploaded name must never set a header");
            }
        }
        assertEquals("evil.pdf", jdbc.queryForObject("SELECT file_name FROM materials WHERE id = ?", String.class, traversal));
        assertEquals("docfdp.exe.pdf", jdbc.queryForObject("SELECT file_name FROM materials WHERE id = ?", String.class, bidi));
        assertEquals("relatório ação ✓.pdf", jdbc.queryForObject("SELECT file_name FROM materials WHERE id = ?", String.class, accents));

        long longName = uploadOk(team, card, ana, "a".repeat(300) + ".pdf");
        String stored = jdbc.queryForObject("SELECT file_name FROM materials WHERE id = ?", String.class, longName);
        assertEquals(255, stored.length());
        assertTrue(stored.endsWith(".pdf"));

        // the storage key is a random UUID, never derived from the file name
        for (String key : jdbc.queryForList("SELECT storage_key FROM materials WHERE kanban_item_id = ?", String.class, card)) {
            assertDoesNotThrow(() -> UUID.fromString(key));
        }
    }

    @Test
    @DisplayName("SEC-M2-06 Two file parts: only the first is stored; a file sent under another field name is a 400")
    void multipartShapes() {
        Team team = newTeam();
        long card = projectId(team, "Projeto partes");
        String boundary = "qa-two-parts";
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.pdf\"\r\nContent-Type: application/pdf\r\n\r\nAAA\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"b.pdf\"\r\nContent-Type: application/pdf\r\n\r\nBBBB\r\n--" + boundary + "--\r\n";
        Resp two = raw("POST", team.path() + "/cards/" + card + "/materials", ana.token(),
                "multipart/form-data; boundary=" + boundary, body.getBytes(StandardCharsets.UTF_8));
        assertEquals(200, two.status(), two.body());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE kanban_item_id = ?", Integer.class, card));

        String wrongName = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"doc\"; filename=\"a.pdf\"\r\nContent-Type: application/pdf\r\n\r\nAAA\r\n--" + boundary + "--\r\n";
        assertEquals(400, raw("POST", team.path() + "/cards/" + card + "/materials", ana.token(),
                "multipart/form-data; boundary=" + boundary, wrongName.getBytes(StandardCharsets.UTF_8)).status());

        String notAFile = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"\r\n\r\nAAA\r\n--" + boundary + "--\r\n";
        assertEquals(400, raw("POST", team.path() + "/cards/" + card + "/materials", ana.token(),
                "multipart/form-data; boundary=" + boundary, notAFile.getBytes(StandardCharsets.UTF_8)).status());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE kanban_item_id = ?", Integer.class, card));
    }

    @Test
    @DisplayName("SEC-M2-07 Malformed multipart (no boundary / truncated) is a client error, not a 500")
    void malformedMultipart() {
        Team team = newTeam();
        long card = projectId(team, "Projeto malformado");
        String path = team.path() + "/cards/" + card + "/materials";

        Resp noBoundary = raw("POST", path, ana.token(), "multipart/form-data", "garbage".getBytes(StandardCharsets.UTF_8));
        assertEquals(400, noBoundary.status(), noBoundary.body());

        Resp truncated = raw("POST", path, ana.token(), "multipart/form-data; boundary=XX",
                ("--XX\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.pdf\"\r\n\r\ntruncated, no closing boundary").getBytes(StandardCharsets.UTF_8));
        assertEquals(400, truncated.status(), truncated.body());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE kanban_item_id = ?", Integer.class, card));
    }

    @Test
    @DisplayName("SEC-M2-08 Reviews: decision enum, comment trimming, markup stored as text, bad JSON is a 400")
    void reviewInputs() {
        Team team = newTeam();
        long card = projectId(team, "Projeto revisao");
        String path = team.path() + "/cards/" + card + "/reviews";

        assertEquals(400, api.post(path, rui.token(), map("decision", "MAYBE")).status());
        assertEquals(400, api.post(path, rui.token(), map("decision", null)).status());
        assertEquals(400, raw("POST", path, rui.token(), "application/json", "{not json".getBytes(StandardCharsets.UTF_8)).status());
        assertEquals(415, raw("POST", path, rui.token(), "text/plain", "x".getBytes(StandardCharsets.UTF_8)).status());
        assertEquals(400, api.post(path, rui.token(), map("decision", "CHANGES_REQUESTED", "comment", "\t \n")).status());

        Resp ok = api.post(path, rui.token(), map("decision", "APPROVED", "comment", "  <img src=x onerror=alert(1)>  "));
        assertEquals(200, ok.status(), ok.body());
        assertEquals("<img src=x onerror=alert(1)>", ok.text("comment"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM card_reviews WHERE kanban_item_id = ?", Integer.class, card));
        assertEquals("APPROVED", api.get(team.path() + "/board", rui.token()).at("columns").get(0).path("cards").get(0).path("reviewStatus").asText());
    }
}
