package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Reviews of project cards (contract 7.10) and reviewStatus on the board. */
class CardReviewsIT extends AbstractIT {

    private String reviewsPath(Team team, long cardId) {
        return team.path() + "/cards/" + cardId + "/reviews";
    }

    @Test
    @DisplayName("IT-REV-01 POST reviews: ADMIN and REVIEWER yes, CREATOR 403, outsider 403, anonymous 401")
    void permissions() {
        Team team = newTeam();
        long card = projectId(team, "Para revisar");

        Resp byReviewer = api.post(reviewsPath(team, card), rui.token(), map("decision", "APPROVED"));
        assertEquals(200, byReviewer.status(), byReviewer.body());
        assertEquals("APPROVED", byReviewer.text("decision"));
        assertEquals(card, byReviewer.num("cardId"));
        assertEquals(rui.id(), byReviewer.num("reviewer.userId"));
        assertEquals("Rui Teste", byReviewer.text("reviewer.name"));
        assertNull(byReviewer.text("comment"));
        assertTrue(byReviewer.text("createdAt").endsWith("Z"));

        assertEquals(200, api.post(reviewsPath(team, card), ana.token(), map("decision", "APPROVED", "comment", "Ótimo")).status());
        assertEquals(403, api.post(reviewsPath(team, card), carlos.token(), map("decision", "APPROVED")).status());
        assertEquals(403, api.post(reviewsPath(team, card), bia.token(), map("decision", "APPROVED")).status());
        assertEquals(401, api.post(reviewsPath(team, card), null, map("decision", "APPROVED")).status());
        assertEquals(404, api.post(reviewsPath(team, 999999999L), rui.token(), map("decision", "APPROVED")).status());
    }

    @Test
    @DisplayName("IT-REV-02 Comment rules: changes need a comment, max 2000, decision required")
    void commentRules() {
        Team team = newTeam();
        long card = projectId(team, "Regras");

        for (Object blank : new Object[]{null, "", "   "}) {
            Resp r = api.post(reviewsPath(team, card), rui.token(), map("decision", "CHANGES_REQUESTED", "comment", blank));
            assertEquals(400, r.status(), r.body());
            assertEquals("Informe um comentário ao solicitar alterações.", r.message());
        }
        assertEquals(400, api.post(reviewsPath(team, card), rui.token(), map("decision", "CHANGES_REQUESTED")).status());

        String tooLong = String.join("", Collections.nCopies(2001, "a"));
        assertEquals(400, api.post(reviewsPath(team, card), rui.token(), map("decision", "APPROVED", "comment", tooLong)).status());
        String max = String.join("", Collections.nCopies(2000, "a"));
        assertEquals(200, api.post(reviewsPath(team, card), rui.token(), map("decision", "CHANGES_REQUESTED", "comment", max)).status());

        assertEquals(400, api.post(reviewsPath(team, card), rui.token(), map("comment", "sem decisão")).status());
        assertEquals(400, api.post(reviewsPath(team, card), rui.token(), map("decision", "MAYBE")).status());
        assertEquals(200, api.post(reviewsPath(team, card), rui.token(), map("decision", "APPROVED", "comment", "  ")).status());
    }

    @Test
    @DisplayName("IT-REV-03 Ideas do not receive reviews (400)")
    void ideaIsRejected() {
        Team team = newTeam();
        long idea = ideaId(team, "Só uma ideia");

        Resp r = api.post(reviewsPath(team, idea), rui.token(), map("decision", "APPROVED"));
        assertEquals(400, r.status(), r.body());
        assertEquals("Ideias não recebem revisão.", r.message());
    }

    @Test
    @DisplayName("IT-REV-04 GET reviews: any member, newest first; outsider 403")
    void listNewestFirst() {
        Team team = newTeam();
        long card = projectId(team, "Histórico");
        assertEquals(0, api.get(reviewsPath(team, card), carlos.token()).size());

        api.post(reviewsPath(team, card), rui.token(), map("decision", "CHANGES_REQUESTED", "comment", "Primeira"));
        api.post(reviewsPath(team, card), ana.token(), map("decision", "APPROVED", "comment", "Segunda"));

        for (Person p : List.of(ana, carlos, rui)) {
            Resp list = api.get(reviewsPath(team, card), p.token());
            assertEquals(200, list.status(), list.body());
            assertEquals(2, list.size());
            assertEquals("Segunda", list.text("0.comment"));
            assertEquals("APPROVED", list.text("0.decision"));
            assertEquals("Primeira", list.text("1.comment"));
        }
        assertEquals(403, api.get(reviewsPath(team, card), bia.token()).status());
        assertEquals(401, api.get(reviewsPath(team, card), null).status());
    }

    @Test
    @DisplayName("IT-REV-05 reviewStatus on the board follows the latest review, per card; the reviewer account can go away")
    void reviewStatusOnBoard() {
        Team team = newTeam();
        long reviewed = projectId(team, "Revisado");
        long untouched = projectId(team, "Intocado");
        long approved = projectId(team, "Aprovado");

        assertEquals("NONE", cardOnBoard(team, reviewed).path("reviewStatus").asString());

        api.post(reviewsPath(team, reviewed), rui.token(), map("decision", "CHANGES_REQUESTED", "comment", "Ajuste"));
        api.post(reviewsPath(team, approved), rui.token(), map("decision", "APPROVED"));
        assertEquals("CHANGES_REQUESTED", cardOnBoard(team, reviewed).path("reviewStatus").asString());
        assertEquals("NONE", cardOnBoard(team, untouched).path("reviewStatus").asString());
        assertEquals("APPROVED", cardOnBoard(team, approved).path("reviewStatus").asString());

        api.post(reviewsPath(team, reviewed), ana.token(), map("decision", "APPROVED"));
        assertEquals("APPROVED", cardOnBoard(team, reviewed).path("reviewStatus").asString());

        // moving the card keeps showing the real status
        Resp moved = api.patch(team.path() + "/cards/" + reviewed + "/move", ana.token(),
                map("columnId", team.columnIds().get(1)));
        assertEquals(200, moved.status(), moved.body());
        assertEquals("APPROVED", moved.text("reviewStatus"));

        api.post(reviewsPath(team, reviewed), rui.token(), map("decision", "CHANGES_REQUESTED", "comment", "De novo"));
        assertEquals("CHANGES_REQUESTED", cardOnBoard(team, reviewed).path("reviewStatus").asString());
    }

    @Test
    @DisplayName("IT-REV-06 Ideas listed in the inbox and new cards carry reviewStatus NONE; deleting a card removes its reviews")
    void ideasAndCascade() {
        Team team = newTeam();
        long idea = ideaId(team, "Ideia");
        Resp ideas = api.get(team.path() + "/ideas", ana.token());
        assertEquals("NONE", ideas.json().get(0).path("reviewStatus").asString());
        assertEquals(idea, ideas.json().get(0).path("id").asLong());

        long card = projectId(team, "Apagável");
        api.post(reviewsPath(team, card), rui.token(), map("decision", "APPROVED"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM card_reviews WHERE kanban_item_id = ?", Integer.class, card));
        assertEquals(204, api.delete(team.path() + "/cards/" + card, ana.token()).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM card_reviews WHERE kanban_item_id = ?", Integer.class, card));
    }

    @Test
    @DisplayName("IT-REV-07 Review of another team's card is 404 and reviewer is kept as null after the account is gone")
    void otherTeamAndReviewerRemoved() {
        Team team = newTeam();
        long card = projectId(team, "Cruzado");
        assertEquals(404, api.post(otherTeam.path() + "/cards/" + card + "/reviews", bia.token(),
                map("decision", "APPROVED")).status());

        Person temp = person("temp");
        addMemberDirect(team.id(), temp, "REVIEWER");
        assertEquals(200, api.post(reviewsPath(team, card), temp.token(), map("decision", "APPROVED")).status());
        jdbc.update("UPDATE card_reviews SET reviewer_id = NULL WHERE kanban_item_id = ?", card);

        Resp list = api.get(reviewsPath(team, card), ana.token());
        assertEquals(1, list.size());
        assertTrue(list.at("0.reviewer").isNull(), list.body());
    }

    private tools.jackson.databind.JsonNode cardOnBoard(Team team, long cardId) {
        Resp board = api.get(team.path() + "/board", ana.token());
        for (tools.jackson.databind.JsonNode column : board.at("columns")) {
            for (tools.jackson.databind.JsonNode card : column.path("cards")) {
                if (card.path("id").asLong() == cardId) return card;
            }
        }
        throw new AssertionError("card " + cardId + " not on the board: " + board.body());
    }
}
