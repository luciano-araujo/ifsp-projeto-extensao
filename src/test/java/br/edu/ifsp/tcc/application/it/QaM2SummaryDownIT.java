package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** QA of M2, summary: the Python service is unreachable (the address points to a port nobody listens on). */
@TestPropertySource(properties = "app.summary.url=http://localhost:1")
class QaM2SummaryDownIT extends AbstractIT {

    @Test
    @DisplayName("IT-SUM-DOWN Service unreachable: 503 with the exact text, old summary kept, nothing saved when there was none")
    void serviceDown() {
        Team team = newTeam();
        long withOld = projectId(team, "Com resumo antigo");
        long withoutOld = projectId(team, "Sem resumo");
        String script = "<p>" + String.join(" ", Collections.nCopies(120, "palavra")) + "</p>";
        for (long card : new long[]{withOld, withoutOld}) {
            assertEquals(200, api.put("/api/v1/kanban/" + card + "/script", ana.token(), map("content", script)).status());
        }
        jdbc.update("INSERT INTO card_summaries (kanban_item_id, summary, generated_at) VALUES (?, 'Resumo antigo', '2026-01-01 10:00:00')", withOld);

        for (long card : new long[]{withOld, withoutOld}) {
            Resp r = api.post(team.path() + "/cards/" + card + "/summary", ana.token(), null);
            assertEquals(503, r.status(), r.body());
            assertEquals("Não foi possível gerar o resumo agora. Tente novamente.", r.message());
        }
        Resp old = api.get(team.path() + "/cards/" + withOld + "/summary", rui.token());
        assertEquals(200, old.status());
        assertEquals("Resumo antigo", old.text("summary"));
        assertEquals(404, api.get(team.path() + "/cards/" + withoutOld + "/summary", rui.token()).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM card_summaries WHERE kanban_item_id = ?", Integer.class, withoutOld));
    }
}
