package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Materials (contract 7.8): upload, list, download, delete, lesson links, materialsCount on cards and the
 * stored files that must not leak. Uses the local storage in target/it-uploads.
 */
class MaterialsIT extends AbstractIT {

    private static final byte[] PDF = "%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.UTF_8);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    // --- Helpers ---

    /** The first bytes the content check expects for the extension of the file name. */
    private static byte[] sample(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> PDF;
            case "png" -> new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
            case "jpg", "jpeg" -> new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
            case "gif" -> "GIF89a..".getBytes(StandardCharsets.UTF_8);
            case "webp" -> new byte[]{'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'};
            case "docx", "xlsx", "pptx" -> new byte[]{'P', 'K', 3, 4, 0};
            case "doc", "xls", "ppt" -> new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
            default -> "texto simples".getBytes(StandardCharsets.UTF_8);
        };
    }

    private String baseUrl() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    private Path storageDir() {
        return Path.of(environment.getProperty("app.storage.local-dir")).toAbsolutePath().normalize();
    }

    private String storageKeyOf(long materialId) {
        return jdbc.queryForObject("SELECT storage_key FROM materials WHERE id = ?", String.class, materialId);
    }

    private Path fileOf(long materialId) {
        return storageDir().resolve(storageKeyOf(materialId));
    }

    private int countRows(long materialId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE id = ?", Integer.class, materialId);
    }

    /** Raw multipart upload: part name, file name, content type and bytes are all under the test's control. */
    private Resp upload(String path, String token, String partName, String fileName, String contentType, byte[] content) {
        String boundary = "----cw" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + partName
                    + "\"; filename=\"" + fileName + "\"\r\nContent-Type: " + contentType + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.write(content);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
        if (token != null) request.header("Authorization", "Bearer " + token);
        try {
            HttpResponse<String> response = http.send(request.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Resp upload(Team team, long cardId, Person by, String fileName, String contentType, byte[] content) {
        return upload(team.path() + "/cards/" + cardId + "/materials", by.token(), "file", fileName, contentType, content);
    }

    private long uploadOk(Team team, long cardId, Person by, String fileName) {
        Resp r = upload(team, cardId, by, fileName, "application/pdf", PDF);
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private record Binary(int status, byte[] body, Map<String, List<String>> headers) {
        String header(String name) {
            return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
                    .findFirst().map(e -> String.join(",", e.getValue())).orElse(null);
        }
    }

    private Binary download(String path, Person who) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET();
        if (who != null) request.header("Authorization", "Bearer " + who.token());
        try {
            HttpResponse<byte[]> r = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            return new Binary(r.statusCode(), r.body(), r.headers().map());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private List<Long> ids(Resp r) {
        List<Long> ids = new ArrayList<>();
        r.json().forEach(m -> ids.add(m.path("id").asLong()));
        return ids;
    }

    private List<Long> idsOf(tools.jackson.databind.JsonNode array) {
        List<Long> ids = new ArrayList<>();
        array.forEach(m -> ids.add(m.path("id").asLong()));
        return ids;
    }

    private long materialsCountOnBoard(Team team, long cardId) {
        Resp board = api.get(team.path() + "/board", ana.token());
        for (var column : board.at("columns")) {
            for (var card : column.path("cards")) {
                if (card.path("id").asLong() == cardId) return card.path("materialsCount").asLong(-1);
            }
        }
        throw new AssertionError("card " + cardId + " not on the board");
    }

    // --- Upload ---

    @Test
    @DisplayName("IT-MAT-01 Upload by ADMIN and CREATOR returns the contract body and stores the file under a UUID key")
    void uploadReturnsContractBody() throws IOException {
        Team team = newTeam();
        long card = projectId(team, "Com material");

        Resp r = upload(team, card, ana, "Roteiro Final.pdf", "application/pdf", PDF);

        assertEquals(200, r.status(), r.body());
        assertTrue(r.num("id") > 0);
        assertDoesNotThrow(() -> UUID.fromString(r.text("uuid")));
        assertEquals(card, r.num("cardId"));
        assertEquals("Roteiro Final.pdf", r.text("fileName"));
        assertEquals("application/pdf", r.text("contentType"));
        assertEquals(PDF.length, r.num("sizeBytes"));
        assertEquals(ana.id(), r.num("uploadedBy.userId"));
        assertEquals("Ana Teste", r.text("uploadedBy.name"));
        assertTrue(r.text("createdAt").matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"), r.text("createdAt"));
        assertEquals(team.path() + "/materials/" + r.num("id") + "/download", r.text("downloadUrl"));

        String key = storageKeyOf(r.num("id"));
        assertDoesNotThrow(() -> UUID.fromString(key), "the storage key is a UUID, not the file name");
        assertFalse(key.contains("Roteiro"));
        assertArrayEquals(PDF, Files.readAllBytes(storageDir().resolve(key)));

        Resp byCreator = upload(team, card, carlos, "slides.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", new byte[]{0x50, 0x4B, 3, 4, 0});
        assertEquals(200, byCreator.status(), byCreator.body());
        assertEquals(carlos.id(), byCreator.num("uploadedBy.userId"));
    }

    @Test
    @DisplayName("IT-MAT-02 Every allowed type is accepted")
    void allowedTypes() {
        Team team = newTeam();
        long card = projectId(team, "Tipos");
        String[][] allowed = {
                {"a.pdf", "application/pdf"}, {"a.doc", "application/msword"},
                {"a.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
                {"a.ppt", "application/vnd.ms-powerpoint"},
                {"a.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
                {"a.xls", "application/vnd.ms-excel"},
                {"a.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
                {"a.txt", "text/plain; charset=UTF-8"}, {"a.png", "image/png"}, {"a.jpg", "image/jpeg"},
                {"a.JPEG", "image/jpeg"}, {"a.webp", "image/webp"}, {"a.gif", "image/gif"}};
        for (String[] type : allowed) {
            Resp r = upload(team, card, ana, type[0], type[1], sample(type[0]));
            assertEquals(200, r.status(), type[0] + " " + r.body());
        }
        assertEquals(allowed.length, api.get(team.path() + "/cards/" + card + "/materials", ana.token()).size());
    }

    @Test
    @DisplayName("IT-MAT-02b Generic or missing content types are accepted for allowed extensions, and the type stored is the extension's")
    void genericContentTypes() {
        Team team = newTeam();
        long card = projectId(team, "Tipos genericos");
        String docx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        Resp octet = upload(team, card, ana, "texto.docx", "application/octet-stream", new byte[]{0x50, 0x4B, 3, 4, 0});
        assertEquals(200, octet.status(), octet.body());
        assertEquals(docx, octet.text("contentType"));
        Resp txt = upload(team, card, ana, "notas.txt", "text/plain", "ol\u00e1".getBytes(StandardCharsets.UTF_8));
        assertEquals(200, txt.status(), txt.body());
        Binary download = download(txt.text("downloadUrl"), ana);
        assertEquals("text/plain;charset=UTF-8", download.header("Content-Type").replace(" ", ""));
        assertEquals("sandbox; default-src 'none'", download.header("Content-Security-Policy"));

        assertEquals(415, upload(team, card, ana, "falso.pdf", "text/html", PDF).status());
    }

    @Test
    @DisplayName("IT-MAT-03 Disallowed extension or mismatching content type: 415 and nothing stored")
    void disallowedTypes() throws IOException {
        Team team = newTeam();
        long card = projectId(team, "Tipos proibidos");
        long filesBefore;
        try (var files = Files.list(storageDir())) {
            filesBefore = files.count();
        }
        String[][] bad = {{"virus.exe", "application/octet-stream"}, {"pagina.html", "text/html"},
                {"imagem.svg", "image/svg+xml"}, {"script.js", "text/javascript"}, {"falso.pdf", "text/html"},
                {"falso.png", "application/pdf"}, {"semextensao", "application/pdf"}, {"arquivo.pdf.exe", "application/pdf"}};
        for (String[] type : bad) {
            Resp r = upload(team, card, ana, type[0], type[1], sample(type[0]));
            assertEquals(415, r.status(), type[0]);
            assertEquals("Tipo de arquivo não permitido.", r.message());
        }
        assertEquals(0, api.get(team.path() + "/cards/" + card + "/materials", ana.token()).size());
        try (var files = Files.list(storageDir())) {
            assertEquals(filesBefore, files.count(), "nothing was written to the storage");
        }
    }

    @Test
    @DisplayName("IT-MAT-04 The file name is sanitized: path parts are dropped, the key is never the name")
    void fileNameIsSanitized() {
        Team team = newTeam();
        long card = projectId(team, "Nomes");

        Resp traversal = upload(team, card, ana, "../../../etc/evil.pdf", "application/pdf", PDF);
        assertEquals(200, traversal.status(), traversal.body());
        assertEquals("evil.pdf", traversal.text("fileName"));

        Resp windows = upload(team, card, ana, "C:\\\\Users\\\\ana\\\\aula 1.pdf", "application/pdf", PDF);
        assertEquals(200, windows.status(), windows.body());
        assertEquals("aula 1.pdf", windows.text("fileName"));

        Resp longName = upload(team, card, ana, "n".repeat(400) + ".pdf", "application/pdf", PDF);
        assertEquals(200, longName.status(), longName.body());
        assertEquals(255, longName.text("fileName").length());
        assertTrue(longName.text("fileName").endsWith(".pdf"));

        assertFalse(Files.exists(storageDir().getParent().resolve("evil.pdf")));
    }

    @Test
    @DisplayName("IT-MAT-05 Limit of 20 MB: 20 MB is accepted, one byte more and 22 MB are 413")
    void sizeLimit() {
        Team team = newTeam();
        long card = projectId(team, "Tamanho");
        byte[] limit = new byte[20 * 1024 * 1024];
        System.arraycopy(PDF, 0, limit, 0, 5);
        Resp ok = upload(team, card, ana, "limite.pdf", "application/pdf", limit);
        assertEquals(200, ok.status(), ok.body());
        assertEquals(limit.length, ok.num("sizeBytes"));

        Resp over = upload(team, card, ana, "acima.pdf", "application/pdf", new byte[limit.length + 1]);
        assertEquals(413, over.status(), over.body());
        assertEquals("O arquivo excede o limite de 20 MB.", over.message());

        Resp huge = upload(team, card, ana, "enorme.pdf", "application/pdf", new byte[22 * 1024 * 1024]);
        assertEquals(413, huge.status(), huge.body());
        assertEquals("O arquivo excede o limite de 20 MB.", huge.message());

        assertEquals(1, api.get(team.path() + "/cards/" + card + "/materials", ana.token()).size());
    }

    @Test
    @DisplayName("IT-MAT-06 Ideas, missing or empty file, wrong content type of the request: 400 / 415")
    void invalidRequests() {
        Team team = newTeam();
        long idea = ideaId(team, "Ideia sem material");
        long card = projectId(team, "Projeto");

        Resp onIdea = upload(team, idea, ana, "a.pdf", "application/pdf", PDF);
        assertEquals(400, onIdea.status());
        assertEquals("Converta a ideia em projeto antes de anexar materiais.", onIdea.message());

        Resp wrongPart = upload(team.path() + "/cards/" + card + "/materials", ana.token(), "arquivo", "a.pdf",
                "application/pdf", PDF);
        assertEquals(400, wrongPart.status(), wrongPart.body());

        Resp empty = upload(team, card, ana, "vazio.pdf", "application/pdf", new byte[0]);
        assertEquals(400, empty.status(), empty.body());

        Resp json = api.post(team.path() + "/cards/" + card + "/materials", ana.token(), map("file", "x"));
        assertEquals(415, json.status(), json.body());

        assertEquals(404, upload(team, 999999999L, ana, "a.pdf", "application/pdf", PDF).status());
    }

    @Test
    @DisplayName("SEC-MAT-01 Upload permissions: REVIEWER 403, non-member 403, no token 401, another team's card 404")
    void uploadPermissions() {
        Team team = newTeam();
        long card = projectId(team, "Permissoes");

        assertEquals(403, upload(team, card, rui, "a.pdf", "application/pdf", PDF).status());
        assertEquals(403, upload(team, card, bia, "a.pdf", "application/pdf", PDF).status());
        assertEquals(403, upload(team, card, loner, "a.pdf", "application/pdf", PDF).status());
        assertEquals(401, upload(team.path() + "/cards/" + card + "/materials", null, "file", "a.pdf",
                "application/pdf", PDF).status());
        // bia is a member of her own team: the card of another team is not found there
        assertEquals(404, upload(otherTeam, card, bia, "a.pdf", "application/pdf", PDF).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE kanban_item_id = ?", Integer.class, card));
    }

    // --- List ---

    @Test
    @DisplayName("IT-MAT-07 List: any member, newest first, empty for ideas; other teams cannot see it")
    void list() {
        Team team = newTeam();
        long card = projectId(team, "Lista");
        long first = uploadOk(team, card, ana, "um.pdf");
        long second = uploadOk(team, card, carlos, "dois.pdf");
        long third = uploadOk(team, card, ana, "tres.pdf");

        for (Person member : List.of(ana, carlos, rui)) {
            Resp r = api.get(team.path() + "/cards/" + card + "/materials", member.token());
            assertEquals(200, r.status(), r.body());
            assertEquals(List.of(third, second, first), ids(r));
            assertEquals("tres.pdf", r.text("0.fileName"));
            assertEquals(team.path() + "/materials/" + third + "/download", r.text("0.downloadUrl"));
        }
        long idea = ideaId(team, "Ideia");
        assertEquals(0, api.get(team.path() + "/cards/" + idea + "/materials", rui.token()).size());

        assertEquals(403, api.get(team.path() + "/cards/" + card + "/materials", bia.token()).status());
        assertEquals(404, api.get(otherTeam.path() + "/cards/" + card + "/materials", bia.token()).status());
        assertEquals(401, api.get(team.path() + "/cards/" + card + "/materials", null).status());
    }

    // --- Download ---

    @Test
    @DisplayName("IT-MAT-08 Download streams the exact bytes: attachment by default, inline on request, nosniff")
    void download() {
        Team team = newTeam();
        long card = projectId(team, "Download");
        Resp up = upload(team, card, ana, "Relatório final.pdf", "application/pdf", PDF);
        assertEquals(200, up.status(), up.body());
        String path = up.text("downloadUrl");
        String encodedName = "Relat%C3%B3rio%20final.pdf";

        for (Person member : List.of(ana, carlos, rui)) {
            Binary attachment = download(path, member);
            assertEquals(200, attachment.status());
            assertArrayEquals(PDF, attachment.body());
            assertEquals("application/pdf", attachment.header("Content-Type"));
            assertEquals("nosniff", attachment.header("X-Content-Type-Options"));
            assertEquals(String.valueOf(PDF.length), attachment.header("Content-Length"));
            String disposition = attachment.header("Content-Disposition");
            assertTrue(disposition.startsWith("attachment;"), disposition);
            assertTrue(disposition.contains("filename*=UTF-8''" + encodedName), disposition);
        }
        Binary inline = download(path + "?inline=true", rui);
        assertEquals(200, inline.status());
        assertTrue(inline.header("Content-Disposition").startsWith("inline;"), inline.header("Content-Disposition"));
        assertTrue(inline.header("Content-Disposition").contains("filename*=UTF-8''" + encodedName));
        assertEquals("nosniff", inline.header("X-Content-Type-Options"));

        Binary explicitAttachment = download(path + "?inline=false", ana);
        assertTrue(explicitAttachment.header("Content-Disposition").startsWith("attachment;"));
        assertEquals(400, download(path + "?inline=maybe", ana).status());
    }

    @Test
    @DisplayName("SEC-MAT-02 Download isolation: non-member 403, no token 401, another team's id 404, unknown id 404")
    void downloadIsolation() {
        Team team = newTeam();
        long card = projectId(team, "Isolamento");
        long material = uploadOk(team, card, ana, "segredo.pdf");
        String path = team.path() + "/materials/" + material + "/download";

        assertEquals(200, download(path, ana).status());
        assertEquals(403, download(path, bia).status());
        assertEquals(403, download(path, loner).status());
        assertEquals(401, download(path, null).status());
        assertEquals(404, download(otherTeam.path() + "/materials/" + material + "/download", bia).status());
        assertEquals(404, download(team.path() + "/materials/999999999/download", ana).status());
    }

    @Test
    @DisplayName("IT-MAT-09 A material whose stored file disappeared downloads as 404, not 500")
    void missingStoredFile() throws IOException {
        Team team = newTeam();
        long card = projectId(team, "Perdido");
        long material = uploadOk(team, card, ana, "perdido.pdf");
        Files.delete(fileOf(material));

        Binary r = download(team.path() + "/materials/" + material + "/download", ana);

        assertEquals(404, r.status());
        assertTrue(new String(r.body(), StandardCharsets.UTF_8).contains("Arquivo não encontrado no armazenamento."));
    }

    // --- Delete ---

    @Test
    @DisplayName("IT-MAT-10 Delete by an editor: 204, row and stored file gone, second delete 404")
    void deleteMaterial() {
        Team team = newTeam();
        long card = projectId(team, "Excluir");
        long byAdmin = uploadOk(team, card, ana, "um.pdf");
        long byCreator = uploadOk(team, card, ana, "dois.pdf");
        Path adminFile = fileOf(byAdmin);
        Path creatorFile = fileOf(byCreator);
        assertTrue(Files.exists(adminFile));

        assertEquals(204, api.delete(team.path() + "/materials/" + byAdmin, ana.token()).status());
        assertEquals(204, api.delete(team.path() + "/materials/" + byCreator, carlos.token()).status());

        assertEquals(0, countRows(byAdmin));
        assertFalse(Files.exists(adminFile));
        assertFalse(Files.exists(creatorFile));
        assertEquals(404, api.delete(team.path() + "/materials/" + byAdmin, ana.token()).status());
        assertEquals(404, download(team.path() + "/materials/" + byAdmin + "/download", ana).status());
        assertEquals(0, api.get(team.path() + "/cards/" + card + "/materials", ana.token()).size());
    }

    @Test
    @DisplayName("SEC-MAT-03 Delete permissions: REVIEWER 403, non-member 403, no token 401, another team's id 404")
    void deletePermissions() {
        Team team = newTeam();
        long card = projectId(team, "Excluir proibido");
        long material = uploadOk(team, card, ana, "fica.pdf");
        Path file = fileOf(material);
        String path = team.path() + "/materials/" + material;

        assertEquals(403, api.delete(path, rui.token()).status());
        assertEquals(403, api.delete(path, bia.token()).status());
        assertEquals(401, api.delete(path, null).status());
        assertEquals(404, api.delete(otherTeam.path() + "/materials/" + material, bia.token()).status());

        assertEquals(1, countRows(material));
        assertTrue(Files.exists(file));
    }

    // --- materialsCount ---

    @Test
    @DisplayName("IT-MAT-11 materialsCount on the board and on move, 0 for a project without files, one value per card")
    void materialsCount() {
        Team team = newTeam();
        long withTwo = projectId(team, "Com dois");
        long withNone = projectId(team, "Sem nenhum");
        long first = uploadOk(team, withTwo, ana, "um.pdf");
        uploadOk(team, withTwo, ana, "dois.pdf");

        assertEquals(2, materialsCountOnBoard(team, withTwo));
        assertEquals(0, materialsCountOnBoard(team, withNone));

        Resp moved = api.patch(team.path() + "/cards/" + withTwo + "/move", ana.token(),
                map("columnId", team.columnIds().get(1)));
        assertEquals(200, moved.status(), moved.body());
        assertEquals(2, moved.num("materialsCount"));

        assertEquals(204, api.delete(team.path() + "/materials/" + first, ana.token()).status());
        assertEquals(1, materialsCountOnBoard(team, withTwo));

        long idea = ideaId(team, "Ideia");
        assertEquals(0, api.get(team.path() + "/ideas", ana.token()).at("0.materialsCount").asLong(-1));
        assertTrue(idea > 0);
    }

    // --- Lesson links ---

    private record Lesson(long courseId, long moduleId, long lessonId) {
        String path(Team team) {
            return team.path() + "/courses/" + courseId + "/modules/" + moduleId + "/lessons/" + lessonId;
        }
    }

    private Lesson newLesson(Team team) {
        String base = team.path() + "/courses";
        long course = api.post(base, ana.token(), map("title", "Curso", "description", "d")).num("id");
        long module = api.post(base + "/" + course + "/modules", ana.token(), map("title", "Modulo")).num("id");
        long lesson = api.post(base + "/" + course + "/modules/" + module + "/lessons", ana.token(),
                map("title", "Aula", "type", "VIDEO")).num("id");
        return new Lesson(course, module, lesson);
    }

    @Test
    @DisplayName("IT-MAT-12 Link a material to a lesson: the lesson comes back with it; twice is 409; unlink is 204")
    void linkAndUnlink() {
        Team team = newTeam();
        long card = projectId(team, "Aula com material");
        long one = uploadOk(team, card, ana, "um.pdf");
        long two = uploadOk(team, card, ana, "dois.pdf");
        Lesson lesson = newLesson(team);
        assertEquals(0, api.get(team.path() + "/courses/" + lesson.courseId(), ana.token())
                .at("modules.0.lessons.0.materials").size());

        Resp linked = api.post(lesson.path(team) + "/materials", carlos.token(), map("materialId", one));
        assertEquals(200, linked.status(), linked.body());
        assertEquals(lesson.lessonId(), linked.num("id"));
        assertEquals(1, linked.at("materials").size());
        assertEquals(one, linked.num("materials.0.id"));
        assertEquals("um.pdf", linked.text("materials.0.fileName"));
        assertEquals(card, linked.num("materials.0.cardId"));
        assertEquals(team.path() + "/materials/" + one + "/download", linked.text("materials.0.downloadUrl"));

        Resp second = api.post(lesson.path(team) + "/materials", ana.token(), map("materialId", two));
        assertEquals(List.of(one, two), idsOf(second.at("materials")));

        Resp twice = api.post(lesson.path(team) + "/materials", ana.token(), map("materialId", one));
        assertEquals(409, twice.status(), twice.body());
        assertEquals("Este material já está vinculado à aula.", twice.message());

        // the course detail loads the materials of every lesson
        Resp detail = api.get(team.path() + "/courses/" + lesson.courseId(), rui.token());
        assertEquals(2, detail.at("modules.0.lessons.0.materials").size());

        // PATCH of the lesson also returns its materials
        Resp patched = api.patch(lesson.path(team), ana.token(), map("title", "Aula renomeada"));
        assertEquals(200, patched.status(), patched.body());
        assertEquals(2, patched.at("materials").size());

        assertEquals(204, api.delete(lesson.path(team) + "/materials/" + one, ana.token()).status());
        assertEquals(1, api.get(team.path() + "/courses/" + lesson.courseId(), ana.token())
                .at("modules.0.lessons.0.materials").size());
        assertEquals(404, api.delete(lesson.path(team) + "/materials/" + one, ana.token()).status());
        // unlinking does not delete the material
        assertEquals(1, countRows(one));
        assertEquals(2, api.get(team.path() + "/cards/" + card + "/materials", ana.token()).size());
    }

    @Test
    @DisplayName("SEC-MAT-04 Linking: REVIEWER 403, unknown or another team's material 400, bad body 400, unknown lesson 404")
    void linkRules() {
        Team team = newTeam();
        long card = projectId(team, "Vinculos");
        long material = uploadOk(team, card, ana, "um.pdf");
        long foreignCard = createProject(otherTeam, bia, "Da outra equipe", otherTeam.columnIds().get(0)).num("id");
        Resp foreign = upload(otherTeam, foreignCard, bia, "dela.pdf", "application/pdf", PDF);
        assertEquals(200, foreign.status(), foreign.body());
        Lesson lesson = newLesson(team);
        String link = lesson.path(team) + "/materials";

        assertEquals(403, api.post(link, rui.token(), map("materialId", material)).status());
        assertEquals(403, api.post(link, bia.token(), map("materialId", material)).status());
        assertEquals(401, api.post(link, null, map("materialId", material)).status());
        Resp foreignLink = api.post(link, ana.token(), map("materialId", foreign.num("id")));
        assertEquals(400, foreignLink.status(), foreignLink.body());
        assertEquals("O material não pertence a esta equipe.", foreignLink.message());
        assertEquals(400, api.post(link, ana.token(), map("materialId", 999999999L)).status());
        Resp noBody = api.post(link, ana.token(), map());
        assertEquals(400, noBody.status(), noBody.body());
        assertEquals("Informe o material.", noBody.message());
        assertEquals(404, api.post(team.path() + "/courses/" + lesson.courseId() + "/modules/" + lesson.moduleId()
                + "/lessons/999999999/materials", ana.token(), map("materialId", material)).status());
        assertEquals(403, api.delete(link + "/" + material, rui.token()).status());

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM course_lesson_materials WHERE lesson_id = ?",
                Integer.class, lesson.lessonId()));
        assertEquals(1, countRows(material));
    }

    @Test
    @DisplayName("IT-MAT-13 Deleting a material also removes its lesson links; deleting a lesson or course keeps the material")
    void linksAndDeletes() {
        Team team = newTeam();
        long card = projectId(team, "Vinculos e exclusoes");
        long material = uploadOk(team, card, ana, "um.pdf");
        Lesson lesson = newLesson(team);
        assertEquals(200, api.post(lesson.path(team) + "/materials", ana.token(), map("materialId", material)).status());

        assertEquals(204, api.delete(team.path() + "/materials/" + material, ana.token()).status());
        assertEquals(0, api.get(team.path() + "/courses/" + lesson.courseId(), ana.token())
                .at("modules.0.lessons.0.materials").size());

        long again = uploadOk(team, card, ana, "dois.pdf");
        assertEquals(200, api.post(lesson.path(team) + "/materials", ana.token(), map("materialId", again)).status());
        assertEquals(204, api.delete(team.path() + "/courses/" + lesson.courseId(), ana.token()).status());
        assertEquals(1, countRows(again));
        assertTrue(Files.exists(fileOf(again)));
    }

    // --- Stored files must not leak ---

    @Test
    @DisplayName("IT-MAT-14 Deleting a card (team route and /kanban/{id}) removes the stored files of its materials")
    void deleteCardRemovesFiles() {
        Team team = newTeam();
        long viaTeam = projectId(team, "Excluir pelo quadro");
        long viaKanban = projectId(team, "Excluir pelo kanban");
        long a = uploadOk(team, viaTeam, ana, "a.pdf");
        long b = uploadOk(team, viaTeam, ana, "b.pdf");
        long c = uploadOk(team, viaKanban, ana, "c.pdf");
        List<Path> files = List.of(fileOf(a), fileOf(b), fileOf(c));
        files.forEach(f -> assertTrue(Files.exists(f)));

        assertEquals(204, api.delete(team.path() + "/cards/" + viaTeam, ana.token()).status());
        assertFalse(Files.exists(files.get(0)));
        assertFalse(Files.exists(files.get(1)));
        assertTrue(Files.exists(files.get(2)));
        assertEquals(0, countRows(a));

        assertEquals(204, api.delete("/api/v1/kanban/" + viaKanban, ana.token()).status());
        assertFalse(Files.exists(files.get(2)));
        assertEquals(0, countRows(c));
    }

    @Test
    @DisplayName("IT-MAT-15 A card delete that is refused keeps the files (reviewer 403)")
    void refusedDeleteKeepsFiles() {
        Team team = newTeam();
        long card = projectId(team, "Fica");
        long material = uploadOk(team, card, ana, "fica.pdf");

        assertEquals(403, api.delete(team.path() + "/cards/" + card, rui.token()).status());

        assertTrue(Files.exists(fileOf(material)));
        assertEquals(1, countRows(material));
    }

    @Test
    @DisplayName("IT-MAT-16 Deleting a column moves its cards: their materials stay, files included")
    void deleteColumnKeepsMaterials() {
        Team team = newTeam();
        long card = projectId(team, "Na coluna");
        long material = uploadOk(team, card, ana, "fica.pdf");

        assertEquals(204, api.delete(team.path() + "/columns/" + team.columnIds().get(0), ana.token()).status());

        assertEquals(1, api.get(team.path() + "/cards/" + card + "/materials", ana.token()).size());
        assertTrue(Files.exists(fileOf(material)));
    }

    @Test
    @DisplayName("IT-MAT-17 Deleting the account of the only member deletes the team and the stored files of all its cards")
    void deleteUserWithPersonalTeamRemovesFiles() {
        Person solo = person("solo");
        Team team = newTeamFor(solo, "Estudio solo", null, null);
        long cardA = createProject(team, solo, "Projeto A", team.columnIds().get(0)).num("id");
        long cardB = createProject(team, solo, "Projeto B", team.columnIds().get(1)).num("id");
        long a = upload(team, cardA, solo, "a.pdf", "application/pdf", PDF).num("id");
        long b = upload(team, cardB, solo, "b.pdf", "application/pdf", PDF).num("id");
        Path fileA = fileOf(a);
        Path fileB = fileOf(b);
        assertTrue(Files.exists(fileA) && Files.exists(fileB));
        // A course with the materials linked to a lesson: the team delete reaches them through two cascade paths
        String courses = team.path() + "/courses";
        long course = api.post(courses, solo.token(), map("title", "Curso", "description", "d")).num("id");
        long module = api.post(courses + "/" + course + "/modules", solo.token(), map("title", "Modulo")).num("id");
        long lesson = api.post(courses + "/" + course + "/modules/" + module + "/lessons", solo.token(),
                map("title", "Aula", "type", "VIDEO")).num("id");
        String link = courses + "/" + course + "/modules/" + module + "/lessons/" + lesson + "/materials";
        assertEquals(200, api.post(link, solo.token(), map("materialId", a)).status());
        assertEquals(200, api.post(link, solo.token(), map("materialId", b)).status());
        // Everything else that hangs from the cards, scripts and materials of the team
        String cardPath = "/api/v1/kanban/" + cardA;
        assertEquals(200, api.put(cardPath + "/script", solo.token(), map("content", "Roteiro da cena um")).status());
        assertEquals(200, api.post(cardPath + "/script/versions", solo.token(), map("content", "Roteiro da cena um")).status());
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM script_versions", Integer.class) > 0);
        long task = taskIdAs(team, cardA, solo, "Gravar");
        Resp assigned = api.patch(team.path() + "/cards/" + cardA + "/tasks/" + task, solo.token(),
                map("assigneeMemberId", team.adminMemberId()));
        assertEquals(200, assigned.status(), assigned.body());
        long idea = createIdea(team, solo, "Ideia votada").num("id");
        assertEquals(200, api.post(team.path() + "/cards/" + idea + "/vote", solo.token(), null).status());
        Resp tagged = api.put("/api/v1/kanban/" + cardB, solo.token(), map("tagId", team.tagIds().get(0)));
        assertEquals(200, tagged.status(), tagged.body());

        assertEquals(204, api.delete("/api/v1/users/" + solo.id(), solo.token()).status());

        assertFalse(Files.exists(fileA));
        assertFalse(Files.exists(fileB));
        assertEquals(0, countRows(a));
        assertEquals(0, countRows(b));
        assertNoOrphans();
    }

    private long taskIdAs(Team team, long cardId, Person by, String title) {
        Resp r = api.post(team.path() + "/cards/" + cardId + "/tasks", by.token(), map("title", title));
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    /** Every foreign key that points to kanban_items, scripts or materials has no row left without its parent. */
    private void assertNoOrphans() {
        List<Map<String, Object>> keys = jdbc.queryForList("SELECT TABLE_NAME AS t, COLUMN_NAME AS c, "
                + "REFERENCED_TABLE_NAME AS r FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ? "
                + "AND REFERENCED_TABLE_NAME IN ('kanban_items', 'scripts', 'materials')", DB_NAME);
        assertTrue(keys.size() >= 8, "foreign keys found: " + keys);
        for (Map<String, Object> key : keys) {
            String sql = "SELECT COUNT(*) FROM `" + key.get("t") + "` x LEFT JOIN `" + key.get("r") + "` p ON p.id = x.`"
                    + key.get("c") + "` WHERE x.`" + key.get("c") + "` IS NOT NULL AND p.id IS NULL";
            assertEquals(0, jdbc.queryForObject(sql, Integer.class), "orphans in " + key);
        }
    }

    @Test
    @DisplayName("IT-MAT-18 Deleting the account of a member of a shared team keeps the materials (uploadedBy becomes null)")
    void deleteUserOfSharedTeamKeepsMaterials() {
        Team team = newTeam();
        Person temp = person("temp");
        addMemberDirect(team.id(), temp, "CREATOR");
        long card = projectId(team, "Compartilhado");
        Resp up = upload(team, card, temp, "dele.pdf", "application/pdf", PDF);
        assertEquals(200, up.status(), up.body());
        long material = up.num("id");
        Path file = fileOf(material);

        assertEquals(204, api.delete("/api/v1/users/" + temp.id(), temp.token()).status());

        Resp list = api.get(team.path() + "/cards/" + card + "/materials", ana.token());
        assertEquals(1, list.size());
        assertNull(list.text("0.uploadedBy"));
        assertTrue(Files.exists(file));
        assertEquals(PDF.length, download(team.path() + "/materials/" + material + "/download", rui).body().length);
    }
}
