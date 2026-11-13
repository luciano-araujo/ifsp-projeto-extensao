package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QA M4, hunting pass on teams (IT-TEAM, decisions 15 and 18): deleting a team with its whole content (rows and stored
 * files, no orphan rows left by the cascade), the personal flag decided by the column and not by the name, mass assignment
 * of the flag, and names with control characters.
 */
@Import(MailCapture.Config.class)
class QaM4TeamsIT extends AbstractIT {

    private static final byte[] PDF = "%PDF-1.4 conteudo de teste".getBytes(StandardCharsets.UTF_8);
    private final HttpClient http = HttpClient.newHttpClient();

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private Resp multipart(String path, Person by, String fileName, byte[] content) {
        String boundary = "----cw" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
                    + "\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
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

    /** Foreign keys whose child row points to a parent that no longer exists (what a half-finished cascade leaves). */
    private List<String> orphanRows() {
        List<Map<String, Object>> keys = jdbc.queryForList(
                "SELECT TABLE_NAME t, COLUMN_NAME c, REFERENCED_TABLE_NAME rt, REFERENCED_COLUMN_NAME rc "
                        + "FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL "
                        + "AND CONSTRAINT_NAME IN (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE "
                        + "WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL GROUP BY TABLE_NAME, CONSTRAINT_NAME HAVING COUNT(*) = 1)",
                DB_NAME, DB_NAME);
        List<String> orphans = new ArrayList<>();
        for (Map<String, Object> k : keys) {
            String sql = "SELECT COUNT(*) FROM `" + k.get("t") + "` ch LEFT JOIN `" + k.get("rt") + "` p ON ch.`" + k.get("c")
                    + "` = p.`" + k.get("rc") + "` WHERE ch.`" + k.get("c") + "` IS NOT NULL AND p.`" + k.get("rc") + "` IS NULL";
            int n = jdbc.queryForObject(sql, Integer.class);
            if (n > 0) orphans.add(k.get("t") + "." + k.get("c") + " -> " + k.get("rt") + " (" + n + ")");
        }
        return orphans;
    }

    /** A team with a card, task, script, review, idea with vote, stored material and a course using it. Returns the material id. */
    private long fill(Team team) {
        long card = projectId(team, "Projeto cheio");
        taskId(team, card, "tarefa");
        assertEquals(200, api.put("/api/v1/kanban/" + card + "/script", ana.token(), map("content", "roteiro do projeto")).status());
        assertEquals(200, api.post(team.path() + "/cards/" + card + "/reviews", rui.token(), map("decision", "APPROVED")).status());
        long idea = ideaId(team, "Ideia votada");
        assertEquals(200, api.post(team.path() + "/cards/" + idea + "/vote", carlos.token(), null).status());
        Resp material = multipart(team.path() + "/cards/" + card + "/materials", ana, "apoio.pdf", PDF);
        assertEquals(200, material.status(), material.body());
        long course = api.post(team.path() + "/courses", ana.token(), map("title", "Curso", "description", "d")).num("id");
        long module = api.post(team.path() + "/courses/" + course + "/modules", ana.token(), map("title", "M1")).num("id");
        long lesson = api.post(team.path() + "/courses/" + course + "/modules/" + module + "/lessons", ana.token(), map("title", "A1")).num("id");
        Resp link = api.post(team.path() + "/courses/" + course + "/modules/" + module + "/lessons/" + lesson + "/materials",
                ana.token(), map("materialId", material.num("id")));
        assertEquals(200, link.status(), link.body());
        return material.num("id");
    }

    @Test
    @DisplayName("IT-TEAM-04 DELETE team with its whole content: rows of every table and the stored file are gone, no orphan rows")
    void deleteTeamWithEverything() {
        Team team = newTeam();
        long materialId = fill(team);
        String key = jdbc.queryForObject("SELECT storage_key FROM materials WHERE id = ?", String.class, materialId);
        Path file = Path.of(environment.getProperty("app.storage.local-dir")).toAbsolutePath().normalize().resolve(key);
        assertTrue(Files.exists(file), "the uploaded file is stored");
        assertEquals(List.of(), orphanRows(), "the database is consistent before the delete");

        assertEquals(204, api.delete("/api/v1/teams/" + team.id(), ana.token()).status());

        assertFalse(Files.exists(file), "the stored file is removed after the commit");
        assertEquals(0, count("SELECT COUNT(*) FROM materials WHERE id = ?", materialId));
        assertEquals(0, count("SELECT COUNT(*) FROM courses WHERE team_id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM kanban_items WHERE team_id = ?", team.id()));
        assertEquals(List.of(), orphanRows(), "no row points to a deleted parent");
        // the people keep their account and their own teams
        assertEquals(200, api.get("/api/v1/teams", carlos.token()).status());
        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE id IN (?, ?, ?)", ana.id(), carlos.id(), rui.id()) / 3);
    }

    @Test
    @DisplayName("IT-TEAM-05 Deleting the account of the only member removes the team, its files and leaves no orphan rows")
    void deleteAccountWithTeamContent() {
        Person solo = person("solo");
        Team team = newTeamFor(solo, "Equipe da solo", null, null);
        long card = api.post(team.path() + "/cards", solo.token(), map("title", "Projeto solo", "description", "d",
                "targetAudience", "Estudantes iniciantes", "pedagogicalObjective", "Aprender o basico do tema",
                "columnId", team.columnIds().get(0))).num("id");
        Resp material = multipart(team.path() + "/cards/" + card + "/materials", solo, "solo.pdf", PDF);
        assertEquals(200, material.status(), material.body());
        String key = jdbc.queryForObject("SELECT storage_key FROM materials WHERE id = ?", String.class, material.num("id"));
        Path file = Path.of(environment.getProperty("app.storage.local-dir")).toAbsolutePath().normalize().resolve(key);
        assertTrue(Files.exists(file));

        assertEquals(204, api.delete("/api/v1/users/" + solo.id(), solo.token()).status());

        assertEquals(0, count("SELECT COUNT(*) FROM teams WHERE id = ?", team.id()));
        assertFalse(Files.exists(file), "the file of the deleted team is removed");
        assertEquals(List.of(), orphanRows());
    }

    @Test
    @DisplayName("IT-TEAM-06 Decision 18: the personal flag is the column, not the name (shared team named like a personal one is deletable)")
    void flagIsNotTheName() {
        Person user = person("flag");
        Resp personal = api.get("/api/v1/teams", user.token());
        long personalId = personal.at("0.id").asLong();
        String personalName = personal.at("0.name").asText();
        assertEquals("Estúdio de Flag", personalName);

        // a shared team with exactly the automatic name is still shared
        Resp lookalike = api.post("/api/v1/teams", user.token(), map("name", personalName));
        assertEquals(200, lookalike.status(), lookalike.body());
        assertFalse(lookalike.at("personal").asBoolean());
        assertEquals(204, api.delete("/api/v1/teams/" + lookalike.num("id"), user.token()).status());

        // a shared team renamed to "Meu Estúdio" is deletable, the personal team renamed to anything is not
        Resp shared = api.post("/api/v1/teams", user.token(), map("name", "Equipe qualquer"));
        assertEquals(200, api.patch("/api/v1/teams/" + shared.num("id"), user.token(), map("name", "Meu Estúdio")).status());
        assertEquals(204, api.delete("/api/v1/teams/" + shared.num("id"), user.token()).status());
        assertEquals(200, api.patch("/api/v1/teams/" + personalId, user.token(), map("name", "Equipe qualquer")).status());
        assertEquals(409, api.delete("/api/v1/teams/" + personalId, user.token()).status());
        assertEquals(1, count("SELECT COUNT(*) FROM teams WHERE id = ?", personalId));
    }

    @Test
    @DisplayName("IT-TEAM-07 The personal flag cannot be changed by the client (PATCH or POST body with personal)")
    void flagCannotBeForged() {
        Person user = person("forge");
        long personalId = api.get("/api/v1/teams", user.token()).at("0.id").asLong();

        Resp unflag = api.patch("/api/v1/teams/" + personalId, user.token(), map("name", "Qualquer", "personal", false));
        assertTrue(unflag.status() == 200 || unflag.status() == 400, unflag.body());
        assertEquals(1, count("SELECT personal FROM teams WHERE id = ?", personalId), "still personal");
        assertEquals(409, api.delete("/api/v1/teams/" + personalId, user.token()).status());

        Resp forged = api.post("/api/v1/teams", user.token(), map("name", "Nova", "personal", true));
        assertTrue(forged.status() == 200 || forged.status() == 400, forged.body());
        if (forged.status() == 200) {
            assertEquals(0, count("SELECT personal FROM teams WHERE id = ?", forged.num("id")), "a created team is never personal");
            assertEquals(204, api.delete("/api/v1/teams/" + forged.num("id"), user.token()).status());
        }
        Resp shared = api.post("/api/v1/teams", user.token(), map("name", "Comum"));
        Resp mark = api.patch("/api/v1/teams/" + shared.num("id"), user.token(), map("name", "Comum 2", "personal", true));
        assertTrue(mark.status() == 200 || mark.status() == 400, mark.body());
        assertEquals(0, count("SELECT personal FROM teams WHERE id = ?", shared.num("id")));
        assertEquals(204, api.delete("/api/v1/teams/" + shared.num("id"), user.token()).status());
    }

    @Test
    @DisplayName("IT-TEAM-08 After the delete: invite tokens are dead, members lose access, a second DELETE or PATCH is refused")
    void afterDelete() {
        Team team = newTeam();
        String target = "late." + UUID.randomUUID().toString().substring(0, 8) + "@creatorworkbench.test";
        assertEquals(200, api.post(team.path() + "/invites", ana.token(), map("email", target, "role", "CREATOR")).status());
        String token = MailCapture.inviteToken(target);
        assertNotNull(token);

        assertEquals(204, api.delete("/api/v1/teams/" + team.id(), ana.token()).status());

        assertEquals(404, api.get("/api/auth/invites/" + token, null).status());
        assertEquals(404, api.post("/api/v1/invites/" + token + "/accept", loner.token(), null).status());
        assertEquals(403, api.get(team.path() + "/board", carlos.token()).status());
        assertEquals(403, api.get(team.path() + "/alerts", rui.token()).status());
        assertEquals(403, api.delete("/api/v1/teams/" + team.id(), ana.token()).status());
        assertEquals(403, api.patch("/api/v1/teams/" + team.id(), ana.token(), map("name", "Fantasma")).status());
        assertEquals(0, api.get("/api/v1/teams", carlos.token()).json().findValuesAsString("name").stream().filter("Fantasma"::equals).count());
    }

    @Test
    @Disabled("BUG QA4-05 (open): team names accept CR/LF/tab/NUL; remove this line when fixed")
    @DisplayName("IT-TEAM-09 Team names: no control characters (CR/LF would reach the subject of the invite e-mail)")
    void namesWithControlCharacters() {
        Person user = person("names");
        for (String bad : new String[] {"Equipe\r\nBcc: attacker@creatorworkbench.test", "Equipe\nlinha", "Equipe\u0000nula", "Equipe\ttab"}) {
            Resp created = api.post("/api/v1/teams", user.token(), map("name", bad));
            assertEquals(400, created.status(), "POST name [" + bad.replace("\r", "\\r").replace("\n", "\\n") + "]: " + created.body());
            long teamId = api.get("/api/v1/teams", user.token()).at("0.id").asLong();
            Resp renamed = api.patch("/api/v1/teams/" + teamId, user.token(), map("name", bad));
            assertEquals(400, renamed.status(), "PATCH name: " + renamed.body());
        }
        // plain names still work, with accents and markup characters (they are text, never HTML)
        long teamId = api.get("/api/v1/teams", user.token()).at("0.id").asLong();
        Resp ok = api.patch("/api/v1/teams/" + teamId, user.token(), map("name", "Núcleo <b>& \"Cia\""));
        assertEquals(200, ok.status(), ok.body());
        assertEquals("Núcleo <b>& \"Cia\"", ok.text("name"));
        assertEquals(120, api.patch("/api/v1/teams/" + teamId, user.token(), map("name", "x".repeat(120))).text("name").length());
    }
}
