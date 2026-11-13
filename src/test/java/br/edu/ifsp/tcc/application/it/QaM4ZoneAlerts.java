package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.usecase.SendDeadlineDigestUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Today" is the date in the configured zone, not the UTC date of the server. The test runs with a zone far east of UTC
 * and with a zone far west of it (see the two subclasses), so that at any wall-clock time at least one of them has a
 * calendar date different from UTC. This is the evening situation of Sao Paulo (UTC already tomorrow) made deterministic.
 */
abstract class QaM4ZoneAlerts extends AbstractIT {

    @Autowired private SendDeadlineDigestUseCase digestUseCase;

    private ZoneId zone() {
        return ZoneId.of(environment.getProperty("app.alerts.timezone"));
    }

    private void setDue(long cardId, LocalDate due) {
        jdbc.update("UPDATE kanban_items SET due_date = ? WHERE id = ?", due, cardId);
    }

    @Test
    @DisplayName("IT-ALR-08 Alerts use the date of the configured zone for overdue, today and the 3-day window, whatever the UTC date is")
    void alertsFollowTheConfiguredZone() {
        Team team = newTeam();
        LocalDate today = LocalDate.now(zone());
        LocalDate utcToday = LocalDate.now(ZoneOffset.UTC);
        // a card for every offset around the window, plus the UTC date itself
        long[] cards = new long[8];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = projectId(team, "Card " + (i - 2));
            setDue(cards[i], today.plusDays(i - 2L));
        }
        long utcCard = projectId(team, "Card UTC");
        setDue(utcCard, utcToday);

        Resp alerts = api.get(team.path() + "/alerts", ana.token());

        assertEquals(200, alerts.status(), alerts.body());
        for (int i = 0; i < cards.length; i++) {
            int offset = i - 2;
            long id = cards[i];
            tools.jackson.databind.JsonNode alert = null;
            for (tools.jackson.databind.JsonNode a : alerts.json()) if (a.path("cardId").asLong() == id) alert = a;
            if (offset > 3) {
                assertNull(alert, "offset " + offset + " is outside the 3-day window in " + zone());
            } else {
                assertNotNull(alert, "offset " + offset + " alerts in " + zone() + ": " + alerts.body());
                assertEquals(offset < 0 ? "CARD_OVERDUE" : "CARD_DUE_SOON", alert.path("type").asString(), "offset " + offset);
                assertEquals(offset, alert.path("daysLeft").asInt());
            }
        }
        int utcOffset = (int) ChronoUnit.DAYS.between(today, utcToday);
        for (tools.jackson.databind.JsonNode a : alerts.json()) {
            if (a.path("cardId").asLong() == utcCard) {
                assertEquals(utcOffset, a.path("daysLeft").asInt(), "the UTC date is just another date, seen from " + zone());
            }
        }
    }

    @Test
    @DisplayName("IT-ALR-09 The digest says 'vence hoje' for the date of the configured zone")
    void digestFollowsTheConfiguredZone() {
        Team team = newTeam();
        long card = projectId(team, "Prazo de hoje na zona");
        setDue(card, LocalDate.now(zone()));
        MailCapture.DIGESTS.clear();

        int sent = digestUseCase.execute();
        MailCapture.awaitDigests(sent);

        String teamName = jdbc.queryForObject("SELECT name FROM teams WHERE id = ?", String.class, team.id());
        List<String> lines = MailCapture.digestsTo(ana.email()).get(0).lines().stream().filter(l -> l.startsWith("[" + teamName + "]")).toList();
        assertEquals(1, lines.size(), lines.toString());
        assertTrue(lines.get(0).contains("vence hoje"), lines.get(0));
    }
}
