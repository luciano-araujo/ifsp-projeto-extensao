package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.service.SummaryGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Summary generation is limited per team and hour (app.summary.max-per-team-hour, 3 here). */
@TestPropertySource(properties = "app.summary.max-per-team-hour=3")
class CardSummaryRateLimitIT extends AbstractIT {

    static final AtomicInteger CALLS = new AtomicInteger();

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        SummaryGenerator countingSummaryGenerator() {
            return text -> {
                CALLS.incrementAndGet();
                return "Resumo gerado.";
            };
        }
    }

    @Test
    @DisplayName("IT-SUM-RL-01 The 4th summary of a team in the hour is 429 with Retry-After and a Portuguese message; another team is not affected")
    void limitPerTeam() {
        Team team = newTeam();
        Team other = newTeam();
        String script = "<p>" + String.join(" ", Collections.nCopies(120, "palavra")) + "</p>";
        long card = projectId(team, "Limite");
        long otherCard = projectId(other, "Outro");
        assertEquals(200, api.put("/api/v1/kanban/" + card + "/script", ana.token(), map("content", script)).status());
        assertEquals(200, api.put("/api/v1/kanban/" + otherCard + "/script", ana.token(), map("content", script)).status());
        int before = CALLS.get();

        for (int i = 0; i < 3; i++) {
            Resp ok = api.post(team.path() + "/cards/" + card + "/summary", ana.token(), null);
            assertEquals(200, ok.status(), ok.body());
        }
        Resp blocked = api.post(team.path() + "/cards/" + card + "/summary", ana.token(), null);
        assertEquals(429, blocked.status(), blocked.body());
        assertEquals("Limite de resumos da equipe atingido. Tente novamente mais tarde.", blocked.message());
        long retryAfter = Long.parseLong(blocked.header("Retry-After"));
        assertTrue(retryAfter > 0 && retryAfter <= 3600, "Retry-After " + retryAfter);
        assertEquals(before + 3, CALLS.get(), "the blocked request never reached the generator");

        // another team keeps working; reads are never limited; a stranger cannot burn the quota (403 before the counter)
        assertEquals(200, api.post(other.path() + "/cards/" + otherCard + "/summary", ana.token(), null).status());
        assertEquals(200, api.get(team.path() + "/cards/" + card + "/summary", ana.token()).status());
        assertEquals(403, api.post(team.path() + "/cards/" + card + "/summary", bia.token(), null).status());
    }
}
