package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Map;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Bugs found by QA on M1 (release/v3 @ 2f660c6), written as the EXPECTED behaviour. They fail today, so they are
 * skipped in the normal run; run them with  .\mvnw.cmd verify -Pit -Dit.knownBugs=true  and remove the condition
 * of a test when its bug is fixed.
 */
@EnabledIfSystemProperty(named = "it.knownBugs", matches = "true")
class KnownBugsIT extends AbstractIT {

    @Test
    @DisplayName("BUG-01 Unknown route -> 404, wrong method -> 405, wrong media type -> 415 (today all answer 401 even with a valid token)")
    void errorDispatchIsNot401() {
        Team team = newTeam();
        assertEquals(404, api.get("/api/v1/nope", ana.token()).status());
        assertEquals(404, api.get("/api/v1/kanban", ana.token()).status(), "removed legacy list route");
        assertEquals(405, api.patch(team.path() + "/board", ana.token(), map()).status());
        assertEquals(405, api.delete(team.path() + "/tags", ana.token()).status());
        assertEquals(415, api.send("POST", team.path() + "/tags", ana.token(), null, Map.of("Content-Type", "text/plain")).status());
    }

    @Test
    @DisplayName("BUG-02 Over-long title/audience/task title -> 400 validation message (today 409 'Registro duplicado')")
    void tooLongIs400() {
        Team team = newTeam();
        long card = projectId(team, "limite");
        String longText = "t".repeat(300);
        assertEquals(400, api.post(team.path() + "/ideas", ana.token(), map("title", longText)).status());
        assertEquals(400, api.post(team.path() + "/ideas", ana.token(), map("title", "x", "targetAudience", longText)).status());
        assertEquals(400, api.post(team.path() + "/cards", ana.token(), map("title", longText,
                "targetAudience", "abc", "pedagogicalObjective", "objetivo longo")).status());
        assertEquals(400, api.post(team.path() + "/cards/" + card + "/tasks", ana.token(), map("title", longText)).status());
        assertEquals(400, api.put("/api/v1/kanban/" + card, ana.token(), map("title", longText)).status());
        assertEquals(400, api.put("/api/v1/kanban/" + card, ana.token(), map("targetAudience", longText)).status());
    }

    @Test
    @DisplayName("BUG-03 Saving a script with null/missing content must not store NULL (snapshot then fails with 409)")
    void nullScriptContent() {
        Team team = newTeam();
        long card = projectId(team, "script nulo");
        String base = "/api/v1/kanban/" + card + "/script";
        Resp saved = api.put(base, ana.token(), map());
        assertTrue(saved.status() == 400 || "".equals(saved.text("content")), "content null stored: " + saved.body());
        Resp get = api.get(base, ana.token());
        assertNotNull(get.text("content"), "GET must keep returning a string (contract 7.2): " + get.body());
        assertEquals(200, api.post(base + "/versions", ana.token(), null).status());
    }

    @Test
    @DisplayName("BUG-04 PUT /kanban/{id} must keep RN01 and a title: blank title/audience/objective -> 400")
    void updateKeepsRn01() {
        Team team = newTeam();
        long card = projectId(team, "rn01");
        assertEquals(400, api.put("/api/v1/kanban/" + card, ana.token(), map("title", "")).status());
        assertEquals(400, api.put("/api/v1/kanban/" + card, ana.token(), map("title", "   ")).status());
        assertEquals(400, api.put("/api/v1/kanban/" + card, ana.token(), map("targetAudience", "")).status());
        assertEquals(400, api.put("/api/v1/kanban/" + card, ana.token(), map("pedagogicalObjective", "  ")).status());
        Resp now = api.get("/api/v1/kanban/" + card, ana.token());
        assertEquals("rn01", now.text("title"));
        assertEquals("Estudantes iniciantes", now.text("targetAudience"));
    }

    @Test
    @DisplayName("BUG-05 Error bodies must not leak Java types, controller signatures or English parser messages")
    void errorBodiesDoNotLeak() {
        Team team = newTeam();
        long card = projectId(team, "vazamento");
        List<Resp> responses = List.of(
                api.post(team.path() + "/tags", ana.token(), "{not json"),
                api.post(team.path() + "/tags", ana.token(), null),
                api.get(team.path() + "/ideas?tagId=abc", ana.token()),
                api.get("/api/v1/teams/abc/board", ana.token()),
                api.put("/api/v1/kanban/" + card, ana.token(), map("dueDate", "2026-02-30")));
        for (Resp r : responses) {
            assertEquals(400, r.status(), r.body());
            String body = r.body();
            for (String leak : List.of("java.", "org.springframework", "br.edu.ifsp", "tools.jackson", "could not be parsed", "Failed to convert")) {
                assertFalse(body.contains(leak), "error body leaks '" + leak + "': " + body);
            }
        }
    }

    @Test
    @DisplayName("BUG-06 columnSince in the move/convert/create response equals the stored history time (today up to 1 s apart)")
    void columnSinceMatchesHistory() throws Exception {
        Team team = newTeam();
        long card = projectId(team, "tempo");
        int mismatches = 0;
        for (int i = 0; i < 10; i++) {
            long col = team.columnIds().get(i % 2 == 0 ? 1 : 0);
            Resp moved = api.patch(team.path() + "/cards/" + card + "/move", ana.token(), map("columnId", col));
            Resp history = api.get(team.path() + "/cards/" + card + "/history", ana.token());
            if (!moved.text("columnSince").equals(history.text("0.enteredAt"))) mismatches++;
            Thread.sleep(150);
        }
        assertEquals(0, mismatches);
    }

    @Test
    @DisplayName("BUG-07 Column and card ordering inputs are validated: negative sortOrder, blank rename -> 400")
    void orderingInputs() {
        Team team = newTeam();
        long card = projectId(team, "ordem");
        long col = team.columnIds().get(0);
        assertEquals(400, api.patch(team.path() + "/columns/" + col, ana.token(), map("sortOrder", -1)).status());
        assertEquals(400, api.patch(team.path() + "/columns/" + col, ana.token(), map("name", "   ")).status());
        assertEquals(400, api.patch(team.path() + "/cards/" + card + "/move", ana.token(), map("columnId", col, "sortOrder", -5)).status());
    }
}
