package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The JVM runs in UTC, so timestamps labelled 'Z' are the real instant whatever the machine zone is. */
class TimestampsIT extends AbstractIT {

    private void assertNow(String value, String field) {
        assertTrue(value != null && value.endsWith("Z"), field + " should end with Z: " + value);
        Duration drift = Duration.between(Instant.parse(value), Instant.now()).abs();
        assertTrue(drift.getSeconds() <= 10, field + " is " + drift + " away from now: " + value);
    }

    @Test
    @DisplayName("createdAt and columnSince are within seconds of the real instant, at creation and when read back")
    void timestampsMatchRealInstant() {
        Team team = newTeam();
        Resp created = createProject(team, ana, "Timestamps", team.columnIds().get(0));
        assertEquals(200, created.status(), created.body());
        assertNow(created.text("createdAt"), "createdAt (create)");
        assertNow(created.text("columnSince"), "columnSince (create)");

        Resp board = api.get(team.path() + "/board", ana.token());
        assertEquals(200, board.status(), board.body());
        String card = board.at("columns.0.cards.0.createdAt").asString();
        assertNow(card, "createdAt (board)");
        Resp newTeam = api.post("/api/v1/teams", ana.token(), Api.map("name", "Equipe UTC"));
        assertNow(newTeam.text("createdAt"), "team createdAt");
    }
}
