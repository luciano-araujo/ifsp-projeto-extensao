package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Board columns: create, rename, reorder (contract 7.5), delete with card transfer and history. */
class ColumnsIT extends AbstractIT {

    private List<Long> boardOrder(Team team) {
        List<Long> ids = new ArrayList<>();
        api.get(team.path() + "/board", ana.token()).at("columns").forEach(c -> ids.add(c.path("id").asLong()));
        return ids;
    }

    private List<Long> ids(Resp r) {
        return ids(r.json());
    }

    private List<Long> ids(tools.jackson.databind.JsonNode array) {
        List<Long> ids = new ArrayList<>();
        array.forEach(c -> ids.add(c.path("id").asLong()));
        return ids;
    }

    @Test
    @DisplayName("IT-COL-01 New team has the 4 default columns in order; board JSON carries cards arrays")
    void defaults() {
        Team team = newTeam();
        Resp board = api.get(team.path() + "/board", rui.token());
        assertEquals(200, board.status());
        assertEquals(List.of("Pesquisa", "Roteiro", "Gravação", "Edição"),
                board.at("columns").valueStream().map(c -> c.path("name").asString()).toList());
        board.at("columns").forEach(c -> assertTrue(c.path("cards").isArray()));
    }

    @Test
    @DisplayName("IT-COL-02 PUT columns/order (ADMIN) returns the columns in the new order, without cards, and persists it")
    void reorder() {
        Team team = newTeam();
        List<Long> reversed = new ArrayList<>(team.columnIds());
        Collections.reverse(reversed);
        long cardId = projectId(team, "Na coluna");

        Resp r = api.put(team.path() + "/columns/order", ana.token(), map("columnIds", reversed));
        assertEquals(200, r.status(), r.body());
        assertEquals(reversed, ids(r));
        assertEquals(0, r.num("0.sortOrder"));
        assertEquals(3, r.num("3.sortOrder"));
        r.json().forEach(c -> assertEquals(0, c.path("cards").size(), "reorder response has no cards"));
        assertEquals(reversed, boardOrder(team));
        // the card stays in its column
        Resp board = api.get(team.path() + "/board", ana.token());
        assertEquals(cardId, board.at("columns.3.cards.0.id").asLong());
        // a column added afterwards goes last
        long created = api.post(team.path() + "/columns", ana.token(), map("name", "Publicacao")).num("id");
        assertEquals(created, boardOrder(team).get(4));
    }

    @Test
    @DisplayName("SEC-COL-03 Only ADMIN reorders/creates/renames/deletes columns: creator and reviewer 403")
    void adminOnly() {
        Team team = newTeam();
        List<Long> order = team.columnIds();
        for (Person p : List.of(carlos, rui)) {
            assertEquals(403, api.put(team.path() + "/columns/order", p.token(), map("columnIds", order)).status());
            assertEquals(403, api.post(team.path() + "/columns", p.token(), map("name", "X")).status());
            assertEquals(403, api.patch(team.path() + "/columns/" + order.get(0), p.token(), map("name", "X")).status());
            assertEquals(403, api.delete(team.path() + "/columns/" + order.get(0), p.token()).status());
        }
        assertEquals(order, boardOrder(team));
        assertEquals(403, api.put(team.path() + "/columns/order", bia.token(), map("columnIds", order)).status());
        assertEquals(401, api.put(team.path() + "/columns/order", null, map("columnIds", order)).status());
    }

    @Test
    @DisplayName("IT-COL-04 columns/order must list exactly the team's columns: missing, extra, repeated, foreign, empty, null are 400")
    void reorderValidation() {
        Team team = newTeam();
        List<Long> c = team.columnIds();
        List<List<Long>> invalid = List.of(
                c.subList(0, 3),
                List.of(c.get(0), c.get(1), c.get(2), c.get(3), otherTeam.columnIds().get(0)),
                List.of(c.get(0), c.get(0), c.get(2), c.get(3)),
                List.of(c.get(0), c.get(1), c.get(2), otherTeam.columnIds().get(0)),
                List.of(c.get(0), c.get(1), c.get(2), 999999999L),
                List.of());
        for (List<Long> bad : invalid) {
            Resp r = api.put(team.path() + "/columns/order", ana.token(), map("columnIds", bad));
            assertEquals(400, r.status(), "ids " + bad + " -> " + r.body());
            assertEquals("A lista deve conter exatamente as colunas da equipe.", r.message());
        }
        assertEquals(400, api.put(team.path() + "/columns/order", ana.token(), map()).status());
        assertEquals(400, api.put(team.path() + "/columns/order", ana.token(), "{\"columnIds\":[1,null]}").status());
        assertEquals(c, boardOrder(team), "order unchanged by rejected requests");
        assertEquals(otherTeam.columnIds(), ids(api.get(otherTeam.path() + "/board", bia.token()).at("columns")),
                "the other team's columns are untouched");
    }

    @Test
    @DisplayName("IT-COL-05 Create/rename: validation, trimming; foreign column id is 404")
    void createAndRename() {
        Team team = newTeam();
        Resp created = api.post(team.path() + "/columns", ana.token(), map("name", "  Publicação  "));
        assertEquals(200, created.status(), created.body());
        assertEquals("Publicação", created.text("name"));
        assertEquals(400, api.post(team.path() + "/columns", ana.token(), map("name", "  ")).status());
        assertEquals(400, api.post(team.path() + "/columns", ana.token(), map()).status());
        assertEquals(400, api.post(team.path() + "/columns", ana.token(), map("name", "x".repeat(81))).status());
        Resp renamed = api.patch(team.path() + "/columns/" + created.num("id"), ana.token(), map("name", "Lançamento"));
        assertEquals("Lançamento", renamed.text("name"));
        assertEquals(404, api.patch(team.path() + "/columns/" + otherTeam.columnIds().get(0), ana.token(), map("name", "Hack")).status());
        assertEquals(404, api.delete(team.path() + "/columns/" + otherTeam.columnIds().get(0), ana.token()).status());
    }

    @Test
    @DisplayName("IT-COL-06 Delete column moves its cards to the target/first column and writes history for each card")
    void deleteMovesCardsAndWritesHistory() {
        Team team = newTeam();
        long colA = team.columnIds().get(0);
        long colB = team.columnIds().get(1);
        long colC = team.columnIds().get(2);
        long one = createProject(team, ana, "Card um", colB).num("id");
        long two = createProject(team, ana, "Card dois", colB).num("id");
        long three = createProject(team, ana, "Tres", colA).num("id");

        assertEquals(204, api.delete(team.path() + "/columns/" + colB + "?targetColumnId=" + colC, ana.token()).status());
        Resp board = api.get(team.path() + "/board", ana.token());
        assertEquals(3, board.at("columns").size());
        assertEquals(1, board.at("columns.0.cards").size());
        assertEquals(2, board.at("columns.1.cards").size(), "columns now: Pesquisa, Gravação (2 cards), Edição");

        for (long id : List.of(one, two)) {
            Resp history = api.get(team.path() + "/cards/" + id + "/history", ana.token());
            assertEquals(2, history.size(), "created in Roteiro, then moved by the column deletion");
            assertEquals("Gravação", history.text("0.columnName"));
            assertEquals(colC, history.num("0.columnId"));
            assertEquals(ana.id(), history.num("0.movedBy.userId"));
            assertEquals("Roteiro", history.text("1.columnName"));
            assertNull(history.text("1.columnId"), "deleted column id is nulled, the name snapshot stays");
        }
        assertEquals(1, api.get(team.path() + "/cards/" + three + "/history", ana.token()).size(), "untouched card");

        // without a target: first remaining column
        assertEquals(204, api.delete(team.path() + "/columns/" + colC, ana.token()).status());
        board = api.get(team.path() + "/board", ana.token());
        assertEquals(3, board.at("columns.0.cards").size());
    }

    @Test
    @DisplayName("IT-COL-07 Delete column rules: last column, target = itself, foreign target")
    void deleteRules() {
        Team team = newTeam();
        long colA = team.columnIds().get(0);
        assertEquals(400, api.delete(team.path() + "/columns/" + colA + "?targetColumnId=" + colA, ana.token()).status());
        assertEquals(404, api.delete(team.path() + "/columns/" + colA + "?targetColumnId=" + otherTeam.columnIds().get(0), ana.token()).status());
        assertEquals(4, boardOrder(team).size());
        for (int i = 0; i < 3; i++) {
            assertEquals(204, api.delete(team.path() + "/columns/" + team.columnIds().get(i), ana.token()).status());
        }
        Resp last = api.delete(team.path() + "/columns/" + team.columnIds().get(3), ana.token());
        assertEquals(400, last.status(), last.body());
        assertEquals("O quadro precisa de pelo menos uma coluna.", last.message());
    }
}
