package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Ideas inbox, cards, conversion, move, delete, history and uuid (IT-05/IT-06/UT-09..UT-21 of the plan, v3 model). */
class IdeasAndCardsIT extends AbstractIT {

    private List<String> titles(Resp r) {
        List<String> titles = new ArrayList<>();
        r.json().forEach(c -> titles.add(c.path("title").asString()));
        return titles;
    }

    @Test
    @DisplayName("IT-05 Creator creates an idea: type IDEA, state in inbox, uuid, no column, not on the board")
    void createIdea() {
        Team team = newTeam();
        Resp r = createIdea(team, carlos, "Minha ideia");
        assertEquals(200, r.status(), r.body());
        assertEquals("IDEA", r.text("type"));
        assertNull(r.text("columnId"));
        assertNull(r.text("columnSince"));
        assertEquals(team.id(), r.num("teamId"));
        assertEquals(0, r.num("votes"));
        assertDoesNotThrow(() -> UUID.fromString(r.text("uuid")));
        assertEquals(1, api.get(team.path() + "/ideas", ana.token()).size());
        Resp board = api.get(team.path() + "/board", ana.token());
        board.at("columns").forEach(c -> assertEquals(0, c.path("cards").size(), "ideas never appear on the board"));
    }

    @Test
    @DisplayName("IT-IDEA-02 Editors edit an idea (title, description, tag) with audience/objective optional; RN01 title >= 3; reviewer 403")
    void editIdeaAndTag() {
        Team team = newTeam();
        long idea = ideaId(team, "Ideia original");
        long podcast = team.tagIds().get(1);
        long curso = team.tagIds().get(2);
        String path = "/api/v1/kanban/" + idea;

        Resp edited = api.put(path, carlos.token(), map("title", "  Ideia editada ", "description", "Nova", "tagId", podcast));
        assertEquals(200, edited.status(), edited.body());
        assertEquals("Ideia editada", edited.text("title"));
        assertEquals(podcast, edited.num("tagId"));
        assertNull(edited.text("targetAudience"));

        Resp keep = api.put(path, ana.token(), map("description", "Outra"));
        assertEquals(podcast, keep.num("tagId"), "absent tagId leaves the tag unchanged");
        assertEquals(curso, api.put(path, ana.token(), map("tagId", curso)).num("tagId"));
        assertEquals(200, api.put(path, ana.token(), map("targetAudience", "", "pedagogicalObjective", "")).status());

        Resp removed = api.put(path, ana.token(), map("tagId", 0));
        assertEquals(200, removed.status(), removed.body());
        assertNull(removed.text("tagId"));

        Resp invalid = api.put(path, ana.token(), map("tagId", 999999999));
        assertEquals(400, invalid.status(), invalid.body());
        assertTrue(invalid.text("message").startsWith("Etiqueta inválida"));
        Team other = newTeam();
        assertEquals(400, api.put(path, ana.token(), map("tagId", other.tagIds().get(0))).status(), "tag of another team");

        for (String shortTitle : new String[]{"ab", "   ", " a "}) {
            Resp r = api.put(path, ana.token(), map("title", shortTitle));
            assertEquals(400, r.status(), r.body());
            assertEquals("O título deve ter pelo menos 3 caracteres.", r.text("message"));
        }
        assertEquals(403, api.put(path, rui.token(), map("tagId", podcast)).status());
        assertEquals("Ideia editada", api.get(team.path() + "/ideas", ana.token()).json().get(0).path("title").asString());
    }

    @Test
    @DisplayName("IT-RN01-TITLE Title needs >= 3 characters on POST ideas, POST cards and convert")
    void titleMinimum() {
        Team team = newTeam();
        String msg = "O título deve ter pelo menos 3 caracteres.";
        Resp idea = api.post(team.path() + "/ideas", ana.token(), map("title", " ab "));
        assertEquals(400, idea.status(), idea.body());
        assertEquals(msg, idea.text("message"));
        Resp card = createProject(team, ana, "ab", team.columnIds().get(0));
        assertEquals(400, card.status(), card.body());
        assertEquals(msg, card.text("message"));
        long id = ideaId(team, "Ideia valida");
        Resp convert = api.post(team.path() + "/cards/" + id + "/convert", ana.token(),
                map("title", "ab", "targetAudience", "Estudantes", "pedagogicalObjective", "Ensinar algo importante"));
        assertEquals(400, convert.status(), convert.body());
        assertEquals(msg, convert.text("message"));
    }

    @Test
    @DisplayName("SEC-ROLE-01 Reviewer cannot create ideas/cards (403) but lists and votes")
    void reviewerReadsAndVotes() {
        Team team = newTeam();
        long ideaId = ideaId(team, "Para votar");
        assertEquals(403, createIdea(team, rui, "Nao pode").status());
        assertEquals(403, createProject(team, rui, "Nao pode", team.columnIds().get(0)).status());
        assertEquals(200, api.get(team.path() + "/ideas", rui.token()).status());
        Resp vote = api.post(team.path() + "/cards/" + ideaId + "/vote", rui.token(), null);
        assertEquals(200, vote.status(), vote.body());
        assertEquals(1, vote.num("votes"));
    }

    @Test
    @DisplayName("IT-IDEA-03 Idea accepts tagId or tag name (case-insensitive); tagId wins; unknown name or foreign tag is 400")
    void ideaTags() {
        Team team = newTeam();
        long podcast = team.tagIds().get(1);

        Resp byId = api.post(team.path() + "/ideas", ana.token(), map("title", "por id", "tagId", podcast));
        assertEquals(200, byId.status(), byId.body());
        assertEquals("Podcast", byId.text("tag"));
        assertEquals(podcast, byId.num("tagId"));
        assertEquals("#1565C0", byId.text("tagColor"));

        Resp byName = api.post(team.path() + "/ideas", ana.token(), map("title", "por nome", "tag", "pOdCaSt"));
        assertEquals(200, byName.status(), byName.body());
        assertEquals(podcast, byName.num("tagId"));

        Resp padded = api.post(team.path() + "/ideas", ana.token(), map("title", "nome com espacos", "tag", "  Curso  "));
        assertEquals(200, padded.status(), padded.body());
        assertEquals("Curso", padded.text("tag"));

        Resp both = api.post(team.path() + "/ideas", ana.token(),
                map("title", "ambos", "tagId", podcast, "tag", "Curso"));
        assertEquals(podcast, both.num("tagId"), "tagId wins over the name");

        assertEquals(400, api.post(team.path() + "/ideas", ana.token(), map("title", "x", "tag", "Inexistente")).status());
        assertEquals(400, api.post(team.path() + "/ideas", ana.token(), map("title", "x", "tagId", 999999999)).status());
        assertEquals(400, api.post(team.path() + "/ideas", ana.token(),
                map("title", "x", "tagId", otherTeam.tagIds().get(0))).status(), "tag of another team");
        assertEquals(400, api.post(team.path() + "/ideas", ana.token(), map("title", "x", "tag", "t".repeat(41))).status());

        Resp none = api.post(team.path() + "/ideas", ana.token(), map("title", "sem tag", "tag", "   "));
        assertEquals(200, none.status(), "blank tag name means no tag");
        assertNull(none.text("tagId"));
    }

    @Test
    @DisplayName("IT-IDEA-04 GET ideas ?q (title/description, case-insensitive), ?tagId, combined filters")
    void ideaFilters() {
        Team team = newTeam();
        long tutorial = team.tagIds().get(0);
        api.post(team.path() + "/ideas", ana.token(), map("title", "Aprendendo Java", "description", "curso basico", "tagId", tutorial));
        api.post(team.path() + "/ideas", ana.token(), map("title", "Podcast de dados", "description", "episodio sobre JAVA"));
        api.post(team.path() + "/ideas", ana.token(), map("title", "Receitas", "description", "culinaria", "tagId", tutorial));

        assertEquals(2, api.get(team.path() + "/ideas?q=java", ana.token()).size());
        assertEquals(2, api.get(team.path() + "/ideas?q=JAVA", ana.token()).size());
        assertEquals(1, api.get(team.path() + "/ideas?q=%20receitas%20", ana.token()).size(), "q is trimmed");
        assertEquals(3, api.get(team.path() + "/ideas?q=", ana.token()).size());
        assertEquals(0, api.get(team.path() + "/ideas?q=inexistente", ana.token()).size());
        assertEquals(2, api.get(team.path() + "/ideas?tagId=" + tutorial, ana.token()).size());
        assertEquals(1, api.get(team.path() + "/ideas?tagId=" + tutorial + "&q=java", ana.token()).size());
        assertEquals(0, api.get(team.path() + "/ideas?tagId=" + otherTeam.tagIds().get(0), ana.token()).size(),
                "a tag of another team matches nothing and leaks nothing");
        assertEquals(0, api.get(team.path() + "/ideas?q=%25", ana.token()).size(), "% is a plain character, not a wildcard");
        assertEquals(0, api.get(team.path() + "/ideas?q=%27%20OR%20%271%27=%271", ana.token()).size(), "SQL injection text is just text");
    }

    @Test
    @DisplayName("IT-IDEA-05 GET ideas ?sort=recent|votes; invalid sort is 400; ties fall back to newest first")
    void ideaSort() {
        Team team = newTeam();
        long first = ideaId(team, "A primeira");
        long second = ideaId(team, "B segunda");
        long third = ideaId(team, "C terceira");
        api.post(team.path() + "/cards/" + first + "/vote", ana.token(), null);
        api.post(team.path() + "/cards/" + first + "/vote", carlos.token(), null);
        api.post(team.path() + "/cards/" + second + "/vote", rui.token(), null);

        assertEquals(List.of("C terceira", "B segunda", "A primeira"), titles(api.get(team.path() + "/ideas", ana.token())));
        assertEquals(List.of("C terceira", "B segunda", "A primeira"), titles(api.get(team.path() + "/ideas?sort=recent", ana.token())));
        Resp byVotes = api.get(team.path() + "/ideas?sort=votes", ana.token());
        assertEquals(List.of("A primeira", "B segunda", "C terceira"), titles(byVotes));
        assertEquals(2, byVotes.num("0.votes"));
        assertTrue(byVotes.at("0.votedByMe").asBoolean());
        assertEquals(List.of("A primeira", "B segunda", "C terceira"), titles(api.get(team.path() + "/ideas?sort=VOTES", ana.token())));

        Resp bad = api.get(team.path() + "/ideas?sort=oldest", ana.token());
        assertEquals(400, bad.status(), bad.body());
        assertEquals("Ordenação inválida. Use recent ou votes.", bad.message());
        assertEquals(400, api.get(team.path() + "/ideas?sort=votes;drop", ana.token()).status());
        assertEquals(400, api.get(team.path() + "/ideas?tagId=abc", ana.token()).status());
        assertNotNull(third);
    }

    @Test
    @DisplayName("IT-06 Vote toggles; a project cannot be voted; one vote per user")
    void voting() {
        Team team = newTeam();
        long idea = ideaId(team, "votavel");
        assertEquals(1, api.post(team.path() + "/cards/" + idea + "/vote", carlos.token(), null).num("votes"));
        Resp again = api.post(team.path() + "/cards/" + idea + "/vote", carlos.token(), null);
        assertEquals(0, again.num("votes"));
        assertFalse(again.at("voted").asBoolean(false));
        long project = projectId(team, "nao votavel");
        assertEquals(400, api.post(team.path() + "/cards/" + project + "/vote", carlos.token(), null).status());
        assertEquals(403, api.post(team.path() + "/cards/" + idea + "/vote", bia.token(), null).status(),
                "non-member cannot vote");
    }

    @Test
    @DisplayName("IT-CARD-01 Direct card needs audience and objective (RN01); lands in the given or first column, with history")
    void createCard() {
        Team team = newTeam();
        Resp missing = api.post(team.path() + "/cards", ana.token(), map("title", "sem campos"));
        assertEquals(400, missing.status());
        assertEquals(400, api.post(team.path() + "/cards", ana.token(),
                map("title", "x", "targetAudience", "  ", "pedagogicalObjective", "obj")).status());

        Resp first = api.post(team.path() + "/cards", ana.token(), map("title", "Projeto 1",
                "targetAudience", "Estudantes", "pedagogicalObjective", "Ensinar o basico"));
        assertEquals(200, first.status(), first.body());
        assertEquals("PROJECT", first.text("type"));
        assertEquals(team.columnIds().get(0), first.num("columnId"));
        assertNotNull(first.text("columnSince"));
        assertEquals(0, first.num("progress"));

        Resp second = createProject(team, ana, "Projeto 2", team.columnIds().get(2));
        assertEquals(team.columnIds().get(2), second.num("columnId"));

        Resp history = api.get(team.path() + "/cards/" + second.num("id") + "/history", rui.token());
        assertEquals(200, history.status());
        assertEquals(1, history.size());
        assertEquals("Gravação", history.text("0.columnName"));
        assertEquals(team.columnIds().get(2), history.num("0.columnId"));
        assertEquals(ana.id(), history.num("0.movedBy.userId"));

        assertEquals(404, createProject(team, ana, "coluna alheia", otherTeam.columnIds().get(0)).status());
    }

    @Test
    @DisplayName("IT-CARD-02 Convert idea: RN01 enforced, history written, columnSince set, tag kept, second convert is 400")
    void convert() {
        Team team = newTeam();
        long tagId = team.tagIds().get(3);
        Resp idea = api.post(team.path() + "/ideas", ana.token(), map("title", "Ideia", "tagId", tagId));
        long id = idea.num("id");

        Resp withoutFields = api.post(team.path() + "/cards/" + id + "/convert", ana.token(), null);
        assertEquals(400, withoutFields.status(), withoutFields.body());
        assertEquals(400, api.post(team.path() + "/cards/" + id + "/convert", ana.token(),
                map("targetAudience", "x")).status());

        long target = team.columnIds().get(1);
        Resp ok = api.post(team.path() + "/cards/" + id + "/convert?targetColumnId=" + target, carlos.token(),
                map("targetAudience", "Estudantes", "pedagogicalObjective", "Ensinar algo importante", "title", "Projeto final"));
        assertEquals(200, ok.status(), ok.body());
        assertEquals("PROJECT", ok.text("type"));
        assertEquals("Projeto final", ok.text("title"));
        assertEquals(target, ok.num("columnId"));
        assertEquals(tagId, ok.num("tagId"));
        assertNotNull(ok.text("columnSince"));

        Resp history = api.get(team.path() + "/cards/" + id + "/history", ana.token());
        assertEquals(1, history.size());
        assertEquals("Roteiro", history.text("0.columnName"));
        assertEquals(carlos.id(), history.num("0.movedBy.userId"));
        assertEquals("Carlos Teste", history.text("0.movedBy.name"));

        assertEquals(400, api.post(team.path() + "/cards/" + id + "/convert", ana.token(), null).status());
        assertEquals(0, api.get(team.path() + "/ideas", ana.token()).size());
        assertEquals(1, api.get(team.path() + "/board", ana.token()).at("columns.1.cards").size());

        assertEquals(403, api.post(team.path() + "/cards/" + id + "/convert", rui.token(), null).status());
        long other = ideaId(team, "outra");
        assertEquals(404, api.post(team.path() + "/cards/" + other + "/convert?targetColumnId="
                + otherTeam.columnIds().get(0), ana.token(), map("targetAudience", "a b c", "pedagogicalObjective", "objetivo longo")).status());
    }

    @Test
    @DisplayName("IT-CARD-03 Move writes history only when the column changes; ideas cannot move; foreign column is 404")
    void move() {
        Team team = newTeam();
        long id = projectId(team, "Movel");
        long colA = team.columnIds().get(0);
        long colB = team.columnIds().get(1);

        Resp inPlace = api.patch(team.path() + "/cards/" + id + "/move", ana.token(), map("columnId", colA, "sortOrder", 3));
        assertEquals(200, inPlace.status(), inPlace.body());
        assertEquals(3, inPlace.num("sortOrder"));
        assertEquals(1, api.get(team.path() + "/cards/" + id + "/history", ana.token()).size(), "reorder in place: no new history");

        Resp moved = api.patch(team.path() + "/cards/" + id + "/move", carlos.token(), map("columnId", colB));
        assertEquals(200, moved.status(), moved.body());
        assertEquals(colB, moved.num("columnId"));
        Resp history = api.get(team.path() + "/cards/" + id + "/history", rui.token());
        assertEquals(2, history.size());
        assertEquals("Roteiro", history.text("0.columnName"), "newest first");
        assertEquals("Pesquisa", history.text("1.columnName"));
        assertEquals(carlos.id(), history.num("0.movedBy.userId"));
        // BUG-06: the move response truncates the time while MySQL rounds it, so they can differ by 1 s
        long drift = Math.abs(java.time.Duration.between(
                java.time.LocalDateTime.parse(moved.text("columnSince").replace("Z", "")),
                java.time.LocalDateTime.parse(history.text("0.enteredAt").replace("Z", ""))).toSeconds());
        assertTrue(drift <= 1, "columnSince is the latest history entry (within 1 s), drift " + drift);

        Resp board = api.get(team.path() + "/board", ana.token());
        assertEquals(0, board.at("columns.0.cards").size());
        assertEquals(id, board.at("columns.1.cards.0.id").asLong());
        assertEquals(history.text("0.enteredAt"), board.at("columns.1.cards.0.columnSince").asString());

        long idea = ideaId(team, "ideia nao move");
        assertEquals(400, api.patch(team.path() + "/cards/" + idea + "/move", ana.token(), map("columnId", colA)).status());
        assertEquals(404, api.patch(team.path() + "/cards/" + id + "/move", ana.token(), map("columnId", otherTeam.columnIds().get(0))).status());
        assertEquals(400, api.patch(team.path() + "/cards/" + id + "/move", ana.token(), map("sortOrder", 1)).status());
        assertEquals(403, api.patch(team.path() + "/cards/" + id + "/move", rui.token(), map("columnId", colA)).status());
    }

    @Test
    @DisplayName("IT-CARD-04 Delete card: 204, gone from board and /kanban, history route 404, reviewer 403, foreign card 404")
    void deleteCard() {
        Team team = newTeam();
        long id = projectId(team, "Para apagar");
        assertEquals(403, api.delete(team.path() + "/cards/" + id, rui.token()).status());
        assertEquals(404, api.delete(otherTeam.path() + "/cards/" + id, bia.token()).status(), "other team's admin on own path: card not found");
        assertEquals(204, api.delete(team.path() + "/cards/" + id, carlos.token()).status());
        assertEquals(404, api.get("/api/v1/kanban/" + id, ana.token()).status());
        assertEquals(404, api.get(team.path() + "/cards/" + id + "/history", ana.token()).status());
        assertEquals(404, api.delete(team.path() + "/cards/" + id, ana.token()).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM card_movements WHERE kanban_item_id = ?", Integer.class, id));
    }

    @Test
    @DisplayName("IT-CARD-05 History: any member reads it; other teams' cards 404; non-members 403; no token 401")
    void historyAccess() {
        Team team = newTeam();
        long id = projectId(team, "Historico");
        for (Person p : List.of(ana, carlos, rui)) {
            assertEquals(200, api.get(team.path() + "/cards/" + id + "/history", p.token()).status());
        }
        assertEquals(404, api.get(otherTeam.path() + "/cards/" + id + "/history", bia.token()).status());
        assertEquals(403, api.get(team.path() + "/cards/" + id + "/history", bia.token()).status());
        assertEquals(401, api.get(team.path() + "/cards/" + id + "/history", null).status());
        assertEquals(404, api.get(team.path() + "/cards/999999999/history", ana.token()).status());
    }

    @Test
    @DisplayName("IT-CARD-06 Progress is computed from tasks (no stored column): 0, 50, 100")
    void progressFromTasks() {
        Team team = newTeam();
        long id = projectId(team, "Progresso");
        long t1 = taskId(team, id, "um");
        long t2 = taskId(team, id, "dois");
        assertEquals(0, api.get(team.path() + "/board", ana.token()).at("columns.0.cards.0.progress").asInt());
        api.patch(team.path() + "/cards/" + id + "/tasks/" + t1 + "/toggle", rui.token(), null);
        Resp board = api.get(team.path() + "/board", ana.token());
        assertEquals(50, board.at("columns.0.cards.0.progress").asInt());
        assertEquals(2, board.at("columns.0.cards.0.tasksTotal").asInt());
        assertEquals(1, board.at("columns.0.cards.0.tasksDone").asInt());
        api.patch(team.path() + "/cards/" + id + "/tasks/" + t2 + "/toggle", ana.token(), null);
        assertEquals(100, api.get(team.path() + "/board", ana.token()).at("columns.0.cards.0.progress").asInt());
    }

    @Test
    @DisplayName("DB-07 / RN05 uuid is present on every card and idea, valid and unique")
    void uuids() {
        Team team = newTeam();
        Set<String> uuids = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            Resp r = i % 2 == 0 ? createIdea(team, ana, "ideia " + i) : createProject(team, ana, "projeto " + i, team.columnIds().get(0));
            String uuid = r.text("uuid");
            assertDoesNotThrow(() -> UUID.fromString(uuid));
            assertTrue(uuids.add(uuid), "duplicate uuid " + uuid);
        }
        Resp board = api.get(team.path() + "/board", ana.token());
        board.at("columns.0.cards").forEach(c -> assertTrue(uuids.contains(c.path("uuid").asString())));
    }

    @Test
    @DisplayName("SEC-TEAM-01 Board, ideas and card routes of another team: non-member 403, foreign card id 404, no token 401")
    void teamIsolation() {
        Team team = newTeam();
        long id = projectId(team, "Privado");
        assertEquals(403, api.get(team.path() + "/board", bia.token()).status());
        assertEquals(403, api.get(team.path() + "/ideas", bia.token()).status());
        assertEquals(403, createIdea(team, bia, "intruso").status());
        assertEquals(403, api.patch(team.path() + "/cards/" + id + "/move", bia.token(), map("columnId", team.columnIds().get(1))).status());
        assertEquals(403, api.get(team.path() + "/board", loner.token()).status());
        assertEquals(401, api.get(team.path() + "/board", null).status());
        // path trick: own team in the path, the other team's card id
        assertEquals(404, api.patch(otherTeam.path() + "/cards/" + id + "/move", bia.token(), map("columnId", otherTeam.columnIds().get(1))).status());
        assertEquals(404, api.post(otherTeam.path() + "/cards/" + id + "/tasks", bia.token(), map("title", "t")).status());
        assertEquals(404, api.patch(otherTeam.path() + "/cards/" + id + "/tasks/1/toggle", bia.token(), null).status());
    }
}
