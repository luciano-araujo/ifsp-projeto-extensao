package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * /api/v1/kanban/{id}/** (member-only, 404 for non-members; editors only for writes) and the script routes:
 * empty script, empty versions, versions, restore and export (contract 7.2 and 7.3).
 * Plan ids: IT-07 (rewritten for 404/403), IT-10, UT-33..UT-38, REG-04, REG-05.
 */
class KanbanScriptIT extends AbstractIT {

    private String words(int n) {
        return String.join(" ", java.util.Collections.nCopies(n, "palavra"));
    }

    @Test
    @DisplayName("IT-KAN-01 GET /kanban/{id}: member reads it (uuid, no progress/tag/user fields); non-members and other teams get 404")
    void getCard() {
        Team team = newTeam();
        long id = projectId(team, "Lido");
        for (Person p : List.of(ana, carlos, rui)) {
            Resp r = api.get("/api/v1/kanban/" + id, p.token());
            assertEquals(200, r.status(), r.body());
            assertEquals("Lido", r.text("title"));
            assertEquals("IN_PRODUCTION", r.text("state"));
            assertDoesNotThrow(() -> UUID.fromString(r.text("uuid")));
            assertTrue(r.json().path("progress").isMissingNode(), "progress is not stored/serialized any more");
            assertTrue(r.json().path("password").isMissingNode());
            assertTrue(r.json().path("user").isMissingNode(), "author entity must not leak");
        }
        assertEquals(404, api.get("/api/v1/kanban/" + id, bia.token()).status());
        assertEquals(404, api.get("/api/v1/kanban/" + id, loner.token()).status());
        assertEquals(404, api.get("/api/v1/kanban/999999999", ana.token()).status());
        assertEquals(401, api.get("/api/v1/kanban/" + id, null).status());
    }

    @Test
    @DisplayName("IT-KAN-02 PUT/DELETE /kanban/{id}: ADMIN and CREATOR yes; REVIEWER 403; non-member 404")
    void updateAndDelete() {
        Team team = newTeam();
        long id = projectId(team, "Original");
        Resp updated = api.put("/api/v1/kanban/" + id, carlos.token(), map("title", "Renomeado", "dueDate", "2031-01-02"));
        assertEquals(200, updated.status(), updated.body());
        assertEquals("Renomeado", updated.text("title"));
        assertEquals("2031-01-02", updated.text("dueDate"));
        assertEquals("Renomeado", api.get(team.path() + "/board", ana.token()).at("columns.0.cards.0.title").asString());
        assertNull(api.put("/api/v1/kanban/" + id, ana.token(), map("dueDate", "")).text("dueDate"));
        assertEquals("Renomeado", api.put("/api/v1/kanban/" + id, ana.token(), map("description", "nova")).text("title"));

        assertEquals(403, api.put("/api/v1/kanban/" + id, rui.token(), map("title", "Hack")).status());
        assertEquals(403, api.delete("/api/v1/kanban/" + id, rui.token()).status());
        assertEquals(404, api.put("/api/v1/kanban/" + id, bia.token(), map("title", "Hack")).status());
        assertEquals(404, api.delete("/api/v1/kanban/" + id, bia.token()).status());
        assertEquals(401, api.put("/api/v1/kanban/" + id, null, map("title", "Hack")).status());
        assertEquals("Renomeado", api.get("/api/v1/kanban/" + id, rui.token()).text("title"));

        assertEquals(400, api.put("/api/v1/kanban/" + id, ana.token(), map("dueDate", "02/01/2031")).status());
        assertEquals(204, api.delete("/api/v1/kanban/" + id, carlos.token()).status());
        assertEquals(404, api.get("/api/v1/kanban/" + id, ana.token()).status());
    }

    @Test
    @DisplayName("IT-KAN-03 Legacy routes are gone: GET/POST /kanban, convert, state/progress, /curriculum/**")
    void legacyRoutesRemoved() {
        Team team = newTeam();
        long id = projectId(team, "Legado");
        for (Resp r : List.of(
                api.get("/api/v1/kanban", ana.token()),
                api.post("/api/v1/kanban", ana.token(), map("title", "x")),
                api.post("/api/v1/kanban/" + id + "/convert", ana.token(), map("targetAudience", "a", "pedagogicalObjective", "b")),
                api.patch("/api/v1/kanban/" + id + "/state", ana.token(), map("state", "REVIEW")),
                api.put("/api/v1/kanban/" + id + "/progress", ana.token(), map("progress", 50)),
                api.get("/api/v1/curriculum/modules?kanbanItemId=" + id, ana.token()),
                api.post("/api/v1/curriculum/modules", ana.token(), map("kanbanItemId", id, "title", "m")),
                api.get("/api/v1/kanban/" + id + "/modules", ana.token()))) {
            // BUG-01: these answer 401 (error dispatch is authenticated) instead of 404/405; KnownBugsIT checks the exact code
            assertTrue(r.status() >= 400 && r.status() < 500, "legacy route must not succeed, answered " + r.status() + ": " + r.body());
        }
        // the card was not touched by those calls
        assertEquals("IN_PRODUCTION", api.get("/api/v1/kanban/" + id, ana.token()).text("state"));
    }

    @Test
    @DisplayName("IT-SCR-01 A card without script: GET returns 200 with empty content and null id; versions returns []")
    void emptyScript() {
        Team team = newTeam();
        long id = projectId(team, "Sem roteiro");
        for (Person p : List.of(ana, carlos, rui)) {
            Resp script = api.get("/api/v1/kanban/" + id + "/script", p.token());
            assertEquals(200, script.status(), script.body());
            assertEquals("", script.text("content"));
            assertNull(script.text("id"));
            assertNull(script.text("createdAt"));
            assertNull(script.text("updatedAt"));
            assertEquals(id, script.num("kanbanItemId"));

            Resp versions = api.get("/api/v1/kanban/" + id + "/script/versions", p.token());
            assertEquals(200, versions.status());
            assertEquals("[]", versions.body().replaceAll("\\s", ""));
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM scripts WHERE kanban_item_id = ?", Integer.class, id),
                "reading an empty script must not create one");
        assertEquals(404, api.get("/api/v1/kanban/" + id + "/script", bia.token()).status());
        assertEquals(404, api.get("/api/v1/kanban/" + id + "/script/versions", loner.token()).status());
        assertEquals(404, api.get("/api/v1/kanban/999999999/script", ana.token()).status());
        assertEquals(401, api.get("/api/v1/kanban/" + id + "/script", null).status());
    }

    @Test
    @DisplayName("IT-10 / UT-33..37 Save script, snapshot, list versions newest first, restore")
    void scriptLifecycle() throws Exception {
        Team team = newTeam();
        long id = projectId(team, "Com roteiro");
        String base = "/api/v1/kanban/" + id + "/script";

        Resp noScriptVersion = api.post(base + "/versions", ana.token(), null);
        assertEquals(400, noScriptVersion.status(), "snapshot of nothing");

        Resp saved = api.put(base, carlos.token(), map("content", "<p>versao 1</p>"));
        assertEquals(200, saved.status(), saved.body());
        assertEquals("<p>versao 1</p>", saved.text("content"));
        assertDoesNotThrow(() -> UUID.fromString(saved.text("uuid")));
        assertNotNull(saved.text("id"));

        Resp v1 = api.post(base + "/versions", ana.token(), null);
        assertEquals(200, v1.status(), v1.body());
        assertEquals("<p>versao 1</p>", v1.text("content"));
        Thread.sleep(1100);
        api.put(base, ana.token(), map("content", "<p>versao 2</p>"));
        Resp v2 = api.post(base + "/versions", ana.token(), null);

        Resp versions = api.get(base + "/versions", rui.token());
        assertEquals(2, versions.size());
        assertEquals(v2.num("id"), versions.num("0.id"), "newest first");
        assertEquals(v1.num("id"), versions.num("1.id"));

        Resp restored = api.post(base + "/versions/" + v1.num("id") + "/restore", ana.token(), null);
        assertEquals(200, restored.status(), restored.body());
        assertEquals("<p>versao 1</p>", restored.text("content"));
        assertEquals("<p>versao 1</p>", api.get(base, rui.token()).text("content"));

        // second save updates the same script (uuid and id stay)
        assertEquals(saved.text("uuid"), api.put(base, ana.token(), map("content", "x")).text("uuid"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM scripts WHERE kanban_item_id = ?", Integer.class, id));

        // version of another card's script
        long other = projectId(team, "Outro");
        api.put("/api/v1/kanban/" + other + "/script", ana.token(), map("content", "o"));
        assertEquals(404, api.post("/api/v1/kanban/" + other + "/script/versions/" + v1.num("id") + "/restore", ana.token(), null).status());
        assertEquals(404, api.post(base + "/versions/999999999/restore", ana.token(), null).status());
    }

    @Test
    @DisplayName("SEC-SCR-02 Script writes: REVIEWER 403; non-member/other team 404; reads allowed for reviewer")
    void scriptPermissions() {
        Team team = newTeam();
        long id = projectId(team, "Permissoes");
        String base = "/api/v1/kanban/" + id + "/script";
        api.put(base, ana.token(), map("content", "<p>conteudo</p>"));
        Resp version = api.post(base + "/versions", ana.token(), null);

        assertEquals(403, api.put(base, rui.token(), map("content", "hack")).status());
        assertEquals(403, api.post(base + "/versions", rui.token(), null).status());
        assertEquals(403, api.post(base + "/versions/" + version.num("id") + "/restore", rui.token(), null).status());
        assertEquals(200, api.get(base, rui.token()).status());
        assertEquals(200, api.get(base + "/versions", rui.token()).status());
        assertEquals(200, api.get(base + "/export", rui.token()).status());
        assertEquals("<p>conteudo</p>", api.get(base, ana.token()).text("content"));

        assertEquals(404, api.put(base, bia.token(), map("content", "hack")).status());
        assertEquals(404, api.post(base + "/versions", bia.token(), null).status());
        assertEquals(404, api.post(base + "/versions/" + version.num("id") + "/restore", bia.token(), null).status());
        assertEquals(404, api.get(base + "/export", bia.token()).status());
        assertEquals(401, api.put(base, null, map("content", "hack")).status());
        assertEquals(401, api.get(base + "/export", null).status());
    }

    @Test
    @DisplayName("IT-EXP-01 Export of a card without script: empty content, 0 words, 1 minute (minimum)")
    void exportEmpty() {
        Team team = newTeam();
        long id = projectId(team, "Export vazio");
        Resp r = api.get("/api/v1/kanban/" + id + "/script/export", rui.token());
        assertEquals(200, r.status(), r.body());
        assertEquals(id, r.num("cardId"));
        assertEquals("Export vazio", r.text("cardTitle"));
        assertEquals("Estudantes iniciantes", r.text("targetAudience"));
        assertEquals("Aprender o basico do tema", r.text("pedagogicalObjective"));
        assertEquals("", r.text("rawContent"));
        assertEquals("", r.text("markdownFormatted"));
        assertEquals(0, r.num("wordCount"));
        assertEquals(1, r.num("estimatedMinutes"));
        assertEquals(8, r.json().size(), "contract 7.3 has exactly these 8 fields: " + r.body());
    }

    @Test
    @DisplayName("IT-EXP-02 Export word count ignores tags and decodes entities; minutes = ceil(words/140), min 1")
    void exportWordsAndMinutes() {
        Team team = newTeam();
        long id = projectId(team, "Export palavras");
        String url = "/api/v1/kanban/" + id + "/script/export";
        String put = "/api/v1/kanban/" + id + "/script";

        int[][] cases = {{1, 1}, {139, 1}, {140, 1}, {141, 2}, {280, 2}, {281, 3}, {1400, 10}, {1401, 11}};
        for (int[] c : cases) {
            api.put(put, ana.token(), map("content", "<p>" + words(c[0]) + "</p>"));
            Resp r = api.get(url, ana.token());
            assertEquals(c[0], r.num("wordCount"), "words for " + c[0]);
            assertEquals(c[1], r.num("estimatedMinutes"), "minutes for " + c[0] + " words");
        }

        api.put(put, ana.token(), map("content", "<h1>Titulo do roteiro</h1><p>Um <strong>dois</strong> tres&nbsp;quatro &amp; cinco</p><ul><li>seis</li><li>sete</li></ul>"));
        Resp r = api.get(url, ana.token());
        assertEquals(11, r.num("wordCount"), "Titulo do roteiro Um dois tres quatro & cinco seis sete");
        assertEquals(1, r.num("estimatedMinutes"));

        api.put(put, ana.token(), map("content", "palavra1 palavra2\n\n  palavra3\tpalavra4"));
        assertEquals(4, api.get(url, ana.token()).num("wordCount"), "plain text without tags");

        api.put(put, ana.token(), map("content", "<p></p><p>   </p><br/>"));
        assertEquals(0, api.get(url, ana.token()).num("wordCount"));
        assertEquals(1, api.get(url, ana.token()).num("estimatedMinutes"));
    }

    @Test
    @DisplayName("IT-EXP-03 Export markdown: headings, bold/italic, lists, quotes; raw HTML is kept in rawContent")
    void exportMarkdown() {
        Team team = newTeam();
        long id = projectId(team, "Export markdown");
        String html = "<h1>Titulo</h1><h2>Sub</h2><p>Texto <strong>forte</strong> e <em>enfase</em>.</p>"
                + "<ul><li>a</li><li>b</li></ul><ol><li>um</li><li>dois</li></ol><blockquote><p>citacao</p></blockquote>";

        // BE-13: the script tag is dropped on save, the rest is kept as is
        api.put("/api/v1/kanban/" + id + "/script", ana.token(), map("content", html + "<script>alert(1)</script>"));
        Resp r = api.get("/api/v1/kanban/" + id + "/script/export", ana.token());
        assertEquals(html, r.text("rawContent"));
        String md = r.text("markdownFormatted");
        assertTrue(md.contains("# Titulo"), md);
        assertTrue(md.contains("## Sub"), md);
        assertTrue(md.contains("Texto **forte** e *enfase*."), md);
        assertTrue(md.contains("- a\n- b"), md);
        assertTrue(md.contains("1. um\n2. dois"), md);
        assertTrue(md.contains("> citacao"), md);
        assertFalse(md.contains("<"), "no HTML tags remain: " + md);
    }
}
