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
import java.util.List;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** M3 (contract 7.9): course edits, reorder, publication check/publish/unpublish, export, RN04 job, upload content check. */
class CoursesM3IT extends AbstractIT {

    private static final byte[] PDF = "%PDF-1.4 teste".getBytes(StandardCharsets.UTF_8);
    private static final String YT = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";

    @Autowired private ArchiveMaterialsJob archiveJob;

    private final HttpClient http = HttpClient.newHttpClient();

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

    private long lesson(Team team, long course, long module, String title, String url) {
        Resp r = api.post(team.path() + "/courses/" + course + "/modules/" + module + "/lessons", ana.token(),
                url == null ? map("title", title) : map("title", title, "contentUrl", url));
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private Resp multipart(String path, Person by, String fileName, byte[] content) {
        String boundary = "----cw" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                    + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
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

    private long uploadMaterial(Team team, long card, String fileName) {
        Resp r = multipart(team.path() + "/cards/" + card + "/materials", ana, fileName, PDF);
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private void link(Team team, long course, long module, long lesson, long material) {
        Resp r = api.post(team.path() + "/courses/" + course + "/modules/" + module + "/lessons/" + lesson + "/materials", ana.token(),
                map("materialId", material));
        assertEquals(200, r.status(), r.body());
    }

    /** A publishable course: one module, one lesson with a YouTube link. */
    private long readyCourse(Team team, String title) {
        long c = course(team, title);
        long m = module(team, c, "Modulo 1");
        lesson(team, c, m, "Aula 1", YT);
        return c;
    }

    // --- Edits ---

    @Test
    @DisplayName("IT-M3-01 PATCH course/module: partial edit, clear description, blank title 400, editors only")
    void editCourseAndModule() {
        Team team = newTeam();
        long c = course(team, "Original");
        long m = module(team, c, "Mod");
        String base = team.path() + "/courses/" + c;

        Resp r = api.patch(base, carlos.token(), map("title", "Novo titulo"));
        assertEquals(200, r.status(), r.body());
        assertEquals("Novo titulo", r.text("title"));
        assertEquals("d", r.text("description"));
        assertEquals("DRAFT", r.text("status"));

        Resp cleared = api.patch(base, ana.token(), map("description", ""));
        assertEquals(200, cleared.status());
        assertNull(cleared.text("description"));
        assertEquals("Novo titulo", cleared.text("title"));

        assertEquals(400, api.patch(base, ana.token(), map("title", " ")).status());
        assertEquals(403, api.patch(base, rui.token(), map("title", "x")).status());
        assertEquals(403, api.patch(base, bia.token(), map("title", "x")).status());
        assertEquals(401, api.patch(base, null, map("title", "x")).status());
        assertEquals(404, api.patch(otherTeam.path() + "/courses/" + c, bia.token(), map("title", "x")).status());

        Resp mod = api.patch(base + "/modules/" + m, ana.token(), map("title", "Renomeado"));
        assertEquals(200, mod.status(), mod.body());
        assertEquals("Renomeado", mod.text("title"));
        assertEquals(400, api.patch(base + "/modules/" + m, ana.token(), map("title", "")).status());
        assertEquals(403, api.patch(base + "/modules/" + m, rui.token(), map("title", "x")).status());
        assertEquals(404, api.patch(base + "/modules/99999999", ana.token(), map("title", "x")).status());
    }

    @Test
    @DisplayName("IT-M3-02 Reorder modules and lessons: returns the ordered list, GET keeps the order, wrong lists are 400")
    void reorder() {
        Team team = newTeam();
        long c = course(team, "Ordem");
        long m1 = module(team, c, "A");
        long m2 = module(team, c, "B");
        long m3 = module(team, c, "C");
        long l1 = lesson(team, c, m1, "L1", null);
        long l2 = lesson(team, c, m1, "L2", null);
        long l3 = lesson(team, c, m1, "L3", null);
        String base = team.path() + "/courses/" + c;

        Resp modules = api.put(base + "/modules/order", carlos.token(), map("moduleIds", List.of(m3, m1, m2)));
        assertEquals(200, modules.status(), modules.body());
        assertEquals(m3, modules.num("0.id"));
        assertEquals(m1, modules.num("1.id"));
        assertEquals(2, modules.num("2.sortOrder"));
        assertEquals(l1, modules.num("1.lessons.0.id"));

        Resp lessons = api.put(base + "/modules/" + m1 + "/lessons/order", ana.token(), map("lessonIds", List.of(l3, l1, l2)));
        assertEquals(200, lessons.status(), lessons.body());
        assertEquals(l3, lessons.num("0.id"));
        assertEquals(0, lessons.num("0.sortOrder"));
        assertEquals(l2, lessons.num("2.id"));

        Resp detail = api.get(base, rui.token());
        assertEquals(m3, detail.num("modules.0.id"));
        assertEquals(m2, detail.num("modules.2.id"));
        assertEquals(l3, detail.num("modules.1.lessons.0.id"));
        assertEquals(l2, detail.num("modules.1.lessons.2.id"));

        assertEquals(400, api.put(base + "/modules/order", ana.token(), map("moduleIds", List.of(m1, m2))).status());
        assertEquals(400, api.put(base + "/modules/order", ana.token(), map("moduleIds", List.of(m1, m1, m2))).status());
        assertEquals(400, api.put(base + "/modules/order", ana.token(), map("moduleIds", List.of(m1, m2, 999999L))).status());
        assertEquals(400, api.put(base + "/modules/order", ana.token(), map("other", 1)).status());
        assertEquals(400, api.put(base + "/modules/" + m1 + "/lessons/order", ana.token(), map("lessonIds", List.of(l1))).status());
        assertEquals(403, api.put(base + "/modules/order", rui.token(), map("moduleIds", List.of(m1, m2, m3))).status());
        assertEquals(403, api.put(base + "/modules/" + m1 + "/lessons/order", rui.token(), map("lessonIds", List.of(l1, l2, l3))).status());
        assertEquals(404, api.put(base + "/modules/99999999/lessons/order", ana.token(), map("lessonIds", List.of())).status());
    }

    @Test
    @DisplayName("IT-M3-03 Lesson description and contentUrl: saved, only http/https accepted, empty text clears, absent keeps")
    void lessonFields() {
        Team team = newTeam();
        long c = course(team, "Aulas");
        long m = module(team, c, "M");
        String base = team.path() + "/courses/" + c + "/modules/" + m + "/lessons";

        Resp created = api.post(base, ana.token(), map("title", "Aula", "description", "Texto", "contentUrl", "http://exemplo.com/v"));
        assertEquals(200, created.status(), created.body());
        assertEquals("Texto", created.text("description"));
        assertEquals("http://exemplo.com/v", created.text("contentUrl"));
        long id = created.num("id");

        for (String bad : new String[]{"ftp://x.com/a", "javascript:alert(1)", "youtube.com/watch", "https://", "http:// x"}) {
            assertEquals(400, api.post(base, ana.token(), map("title", "X", "contentUrl", bad)).status(), bad);
            assertEquals(400, api.patch(base + "/" + id, ana.token(), map("contentUrl", bad)).status(), bad);
        }

        Resp titleOnly = api.patch(base + "/" + id, ana.token(), map("title", "Outro"));
        assertEquals("Texto", titleOnly.text("description"));
        assertEquals("http://exemplo.com/v", titleOnly.text("contentUrl"));

        Resp cleared = api.patch(base + "/" + id, ana.token(), map("description", "", "contentUrl", ""));
        assertEquals(200, cleared.status(), cleared.body());
        assertNull(cleared.text("description"));
        assertNull(cleared.text("contentUrl"));
        assertEquals(403, api.patch(base + "/" + id, rui.token(), map("description", "x")).status());

        // Whitespace-only clears like an empty text; 501 characters is refused
        assertEquals(200, api.patch(base + "/" + id, ana.token(), map("contentUrl", "https://vimeo.com/1")).status());
        Resp blank = api.patch(base + "/" + id, ana.token(), map("contentUrl", "   "));
        assertEquals(200, blank.status(), blank.body());
        assertNull(blank.text("contentUrl"));
        String tooLong = "https://exemplo.com/" + "a".repeat(481);
        assertEquals(400, api.patch(base + "/" + id, ana.token(), map("contentUrl", tooLong)).status());
        assertEquals(400, api.post(base, ana.token(), map("title", "X", "contentUrl", tooLong)).status());
    }

    // --- Publication ---

    @Test
    @DisplayName("IT-M3-04 publication-check: NO_MODULES, EMPTY_MODULE, LESSON_WITHOUT_MATERIAL, INVALID_LINK and publishable")
    void publicationCheck() {
        Team team = newTeam();
        long c = course(team, "Checagem");
        String check = team.path() + "/courses/" + c + "/publication-check";

        Resp none = api.get(check, rui.token());
        assertEquals(200, none.status(), none.body());
        assertFalse(none.json().path("publishable").asBoolean());
        assertEquals("NO_MODULES", none.text("problems.0.type"));
        assertEquals(0, none.at("problems.0.ids").size());

        long empty = module(team, c, "Modulo vazio");
        long full = module(team, c, "Modulo cheio");
        long noMaterial = lesson(team, c, full, "Sem nada", null);
        long bad = lesson(team, c, full, "Link ruim", "https://exemplo.com/video");
        lesson(team, c, full, "Link bom", YT);
        lesson(team, c, full, "Vimeo", "https://vimeo.com/123456");
        lesson(team, c, full, "LMS", "https://classroom.google.com/c/abc");

        Resp problems = api.get(check, ana.token());
        assertFalse(problems.json().path("publishable").asBoolean());
        assertEquals(3, problems.at("problems").size());
        assertEquals("EMPTY_MODULE", problems.text("problems.0.type"));
        assertEquals(empty, problems.num("problems.0.ids.0"));
        assertEquals("Modulo vazio", problems.text("problems.0.titles.0"));
        assertEquals("LESSON_WITHOUT_MATERIAL", problems.text("problems.1.type"));
        assertEquals(noMaterial, problems.num("problems.1.ids.0"));
        assertEquals("INVALID_LINK", problems.text("problems.2.type"));
        assertEquals(bad, problems.num("problems.2.ids.0"));
        assertTrue(problems.text("problems.2.message").contains("Link ruim"));

        assertEquals(403, api.get(check, bia.token()).status());
        assertEquals(404, api.get(otherTeam.path() + "/courses/" + c + "/publication-check", bia.token()).status());
        assertEquals(401, api.get(check, null).status());
    }

    @Test
    @DisplayName("IT-M3-05 Publish flow: 400 with problems, material instead of link, publish, edit while published, unpublish")
    void publishFlow() {
        Team team = newTeam();
        long card = projectId(team, "Projeto do curso");
        long c = course(team, "Publicar");
        long m = module(team, c, "M1");
        long l = lesson(team, c, m, "Aula sem link", null);
        String base = team.path() + "/courses/" + c;

        Resp refused = api.post(base + "/publish", ana.token(), null);
        assertEquals(400, refused.status(), refused.body());
        assertTrue(refused.message().startsWith("O curso não pode ser publicado."));
        assertTrue(refused.message().contains("Aula sem link"));
        assertEquals("DRAFT", api.get(base, ana.token()).text("status"));

        assertEquals(403, api.post(base + "/publish", rui.token(), null).status());
        assertEquals(403, api.post(base + "/unpublish", rui.token(), null).status());

        link(team, c, m, l, uploadMaterial(team, card, "apostila.pdf"));
        Resp ok = api.post(base + "/publish", carlos.token(), null);
        assertEquals(200, ok.status(), ok.body());
        assertEquals("PUBLISHED", ok.text("status"));
        assertNotNull(ok.text("publishedAt"));
        assertTrue(api.get(base + "/publication-check", ana.token()).json().path("publishable").asBoolean());

        // Editing a published course is allowed and keeps it published
        assertEquals(200, api.patch(base, ana.token(), map("title", "Publicado e editado")).status());
        lesson(team, c, m, "Nova aula", YT);
        Resp still = api.get(base, ana.token());
        assertEquals("PUBLISHED", still.text("status"));
        assertEquals(ok.text("publishedAt"), still.text("publishedAt"));

        Resp again = api.post(base + "/publish", ana.token(), null);
        assertEquals(200, again.status());
        assertEquals(ok.text("publishedAt"), again.text("publishedAt"));

        Resp back = api.post(base + "/unpublish", ana.token(), null);
        assertEquals(200, back.status(), back.body());
        assertEquals("DRAFT", back.text("status"));
        assertNull(back.text("publishedAt"));
        assertEquals("DRAFT", api.get(team.path() + "/courses", ana.token()).text("0.status"));
    }

    @Test
    @DisplayName("IT-M3-06 An invalid link blocks publication; fixing it allows it")
    void invalidLinkBlocksPublication() {
        Team team = newTeam();
        long c = course(team, "Link");
        long m = module(team, c, "M");
        long l = lesson(team, c, m, "Aula", "http://www.youtube.com/watch?v=dQw4w9WgXcQ");
        String base = team.path() + "/courses/" + c;

        Resp refused = api.post(base + "/publish", ana.token(), null);
        assertEquals(400, refused.status());
        assertTrue(refused.message().contains("link inválido"));

        assertEquals(200, api.patch(base + "/modules/" + m + "/lessons/" + l, ana.token(), map("contentUrl", YT)).status());
        assertEquals(200, api.post(base + "/publish", ana.token(), null).status());
    }

    // --- Export ---

    @Test
    @DisplayName("IT-M3-07 Export csv and json: every member, header, separator, file name, formats")
    void export() {
        Team team = newTeam();
        long card = projectId(team, "Projeto export");
        long c = course(team, "Exportar");
        long m = module(team, c, "Introdução");
        long l = lesson(team, c, m, "Aula; com ponto e vírgula", YT);
        long l2 = lesson(team, c, m, "Aula com arquivo", null);
        link(team, c, m, l2, uploadMaterial(team, card, "slides.pdf"));
        module(team, c, "Módulo vazio");
        String base = team.path() + "/courses/" + c + "/export";

        for (Person who : List.of(ana, carlos, rui)) {
            Resp csv = api.get(base + "?format=csv", who.token());
            assertEquals(200, csv.status(), csv.body());
            assertTrue(csv.header("Content-Type").toLowerCase().startsWith("text/csv"));
            assertTrue(csv.header("Content-Type").toLowerCase().contains("utf-8"));
            assertTrue(csv.header("Content-Disposition").contains("filename=\"curso-" + c + ".csv\""));
            String[] lines = csv.body().split("\r\n");
            assertTrue(csv.body().startsWith("﻿"), "UTF-8 BOM");
            lines[0] = lines[0].substring(1);
            assertEquals("modulo_ordem;modulo_titulo;aula_ordem;aula_titulo;aula_tipo;aula_publicada;aula_link;projeto_id;materiais", lines[0]);
            assertEquals("1;Introdução;1;\"Aula; com ponto e vírgula\";VIDEO;false;" + YT + ";;", lines[1]);
            assertEquals("1;Introdução;2;Aula com arquivo;VIDEO;false;;;slides.pdf", lines[2]);
            assertEquals("2;Módulo vazio;;;;;;;", lines[3]);
            assertEquals(4, lines.length);
        }

        Resp json = api.get(base + "?format=json", rui.token());
        assertEquals(200, json.status(), json.body());
        assertTrue(json.header("Content-Type").startsWith("application/json"));
        assertTrue(json.header("Content-Disposition").contains("curso-" + c + ".json"));
        assertEquals("Exportar", json.text("title"));
        assertEquals(l, json.num("modules.0.lessons.0.id"));
        assertEquals("slides.pdf", json.text("modules.0.lessons.1.materials.0.fileName"));

        assertEquals(200, api.get(base, ana.token()).status());
        assertEquals(400, api.get(base + "?format=xml", ana.token()).status());
        assertEquals(403, api.get(base + "?format=csv", bia.token()).status());
        assertEquals(404, api.get(otherTeam.path() + "/courses/" + c + "/export", bia.token()).status());
        assertEquals(401, api.get(base, null).status());
    }

    // --- RN04 job ---

    @Test
    @DisplayName("IT-M3-08 RN04 job: materials of courses published 30+ days ago are marked archived, others are not")
    void archiveJob() {
        Team team = newTeam();
        long card = projectId(team, "Projeto arquivo");

        long old = readyCourse(team, "Antigo");
        long oldModule = api.get(team.path() + "/courses/" + old, ana.token()).num("modules.0.id");
        long oldLesson = api.get(team.path() + "/courses/" + old, ana.token()).num("modules.0.lessons.0.id");
        long oldMaterial = uploadMaterial(team, card, "antigo.pdf");
        link(team, old, oldModule, oldLesson, oldMaterial);
        assertEquals(200, api.post(team.path() + "/courses/" + old + "/publish", ana.token(), null).status());

        long recent = readyCourse(team, "Recente");
        long recentModule = api.get(team.path() + "/courses/" + recent, ana.token()).num("modules.0.id");
        long recentLesson = api.get(team.path() + "/courses/" + recent, ana.token()).num("modules.0.lessons.0.id");
        long recentMaterial = uploadMaterial(team, card, "recente.pdf");
        link(team, recent, recentModule, recentLesson, recentMaterial);
        assertEquals(200, api.post(team.path() + "/courses/" + recent + "/publish", ana.token(), null).status());

        long draft = readyCourse(team, "Rascunho");
        long draftModule = api.get(team.path() + "/courses/" + draft, ana.token()).num("modules.0.id");
        long draftLesson = api.get(team.path() + "/courses/" + draft, ana.token()).num("modules.0.lessons.0.id");
        long draftMaterial = uploadMaterial(team, card, "rascunho.pdf");
        link(team, draft, draftModule, draftLesson, draftMaterial);
        jdbc.update("UPDATE courses SET published_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 40 DAY) WHERE id = ?", old);
        jdbc.update("UPDATE courses SET published_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 DAY) WHERE id = ?", recent);
        long unlinked = uploadMaterial(team, card, "solto.pdf");

        archiveJob.run();

        assertNotNull(archivedAt(oldMaterial));
        assertNull(archivedAt(recentMaterial));
        assertNull(archivedAt(draftMaterial));
        assertNull(archivedAt(unlinked));

        Object first = archivedAt(oldMaterial);
        archiveJob.run();
        assertEquals(first, archivedAt(oldMaterial), "a second run leaves archived rows alone");
        assertEquals(200, api.get(team.path() + "/materials/" + oldMaterial + "/download", ana.token()).status());
    }

    private Object archivedAt(long materialId) {
        return jdbc.queryForObject("SELECT archived_at FROM materials WHERE id = ?", Object.class, materialId);
    }

    // --- Upload content check ---

    @Test
    @DisplayName("IT-M3-09 Upload content check: bytes must match the extension (415), real signatures pass")
    void uploadContentCheck() {
        Team team = newTeam();
        long card = projectId(team, "Conteudo");
        String path = team.path() + "/cards/" + card + "/materials";

        Resp fake = multipart(path, ana, "falso.pdf", "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
        assertEquals(415, fake.status(), fake.body());
        assertEquals("Tipo de arquivo não permitido.", fake.message());
        assertEquals(415, multipart(path, ana, "falso.png", PDF).status());
        assertEquals(415, multipart(path, ana, "falso.docx", PDF).status());
        assertEquals(415, multipart(path, ana, "binario.txt", new byte[]{'a', 0, 'b'}).status());
        assertEquals(415, multipart(path, ana, "webp.webp", "RIFF1234WAVE".getBytes(StandardCharsets.UTF_8)).status());
        assertEquals(0, api.get(path, ana.token()).size());

        List<Resp> ok = new ArrayList<>();
        ok.add(multipart(path, ana, "ok.pdf", PDF));
        ok.add(multipart(path, ana, "ok.png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0}));
        ok.add(multipart(path, ana, "ok.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}));
        ok.add(multipart(path, ana, "ok.gif", "GIF87a....".getBytes(StandardCharsets.UTF_8)));
        ok.add(multipart(path, ana, "ok.webp", "RIFF1234WEBP".getBytes(StandardCharsets.UTF_8)));
        ok.add(multipart(path, ana, "ok.xlsx", new byte[]{'P', 'K', 3, 4}));
        ok.add(multipart(path, ana, "ok.ppt", new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1}));
        ok.add(multipart(path, ana, "ok.txt", "olá mundo".getBytes(StandardCharsets.UTF_8)));
        for (Resp r : ok) assertEquals(200, r.status(), r.body());
        assertEquals(ok.size(), api.get(path, ana.token()).size());
    }
}
