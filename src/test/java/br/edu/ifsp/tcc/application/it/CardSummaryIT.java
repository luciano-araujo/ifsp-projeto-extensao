package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.service.SummaryGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Summary of a project script (contract 7.11). The Python service is replaced by a stub bean; no LLM is called. */
class CardSummaryIT extends AbstractIT {

    static final AtomicBoolean DOWN = new AtomicBoolean(false);
    static final AtomicReference<String> LAST_TEXT = new AtomicReference<>();
    static final AtomicReference<String> NEXT_SUMMARY = new AtomicReference<>("Resumo gerado.");

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        SummaryGenerator stubSummaryGenerator() {
            return text -> {
                LAST_TEXT.set(text);
                if (DOWN.get()) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Não foi possível gerar o resumo agora. Tente novamente.");
                }
                return NEXT_SUMMARY.get();
            };
        }
    }

    @BeforeEach
    void reset() {
        DOWN.set(false);
        LAST_TEXT.set(null);
        NEXT_SUMMARY.set("Resumo gerado.");
    }

    private String words(int n) {
        return String.join(" ", Collections.nCopies(n, "palavra"));
    }

    private String summaryPath(Team team, long cardId) {
        return team.path() + "/cards/" + cardId + "/summary";
    }

    private void saveScript(long cardId, String html) {
        Resp r = api.put("/api/v1/kanban/" + cardId + "/script", ana.token(), map("content", html));
        assertEquals(200, r.status(), r.body());
    }

    @Test
    @DisplayName("IT-SUM-01 GET summary: 404 before, 200 after generating; any member reads it")
    void getBeforeAndAfter() {
        Team team = newTeam();
        long card = projectId(team, "Resumível");

        Resp none = api.get(summaryPath(team, card), carlos.token());
        assertEquals(404, none.status());
        assertEquals("Nenhum resumo gerado para este projeto.", none.message());

        saveScript(card, "<p>" + words(150) + "</p>");
        Resp generated = api.post(summaryPath(team, card), ana.token(), null);
        assertEquals(200, generated.status(), generated.body());
        assertEquals(card, generated.num("cardId"));
        assertEquals("Resumo gerado.", generated.text("summary"));
        assertEquals("SCRIPT", generated.text("basedOn"));
        assertTrue(generated.text("generatedAt").endsWith("Z"));

        for (Person p : new Person[]{ana, carlos, rui}) {
            Resp got = api.get(summaryPath(team, card), p.token());
            assertEquals(200, got.status(), got.body());
            assertEquals("Resumo gerado.", got.text("summary"));
            assertEquals(generated.text("generatedAt"), got.text("generatedAt"));
        }
        assertEquals(403, api.get(summaryPath(team, card), bia.token()).status());
        assertEquals(401, api.get(summaryPath(team, card), null).status());
    }

    @Test
    @DisplayName("IT-SUM-02 POST summary sends the script as plain text and overwrites the row")
    void plainTextAndOverwrite() {
        Team team = newTeam();
        long card = projectId(team, "Texto puro");
        saveScript(card, "<h1>Título</h1><p><strong>" + words(60) + "</strong></p><ul><li>" + words(60) + "</li></ul>");

        assertEquals(200, api.post(summaryPath(team, card), carlos.token(), null).status());
        assertEquals("Título: Texto puro\nPúblico-alvo: Estudantes iniciantes\nObjetivo: Aprender o basico do tema\n\nTítulo\n" + words(60) + "\n" + words(60), LAST_TEXT.get());

        NEXT_SUMMARY.set("Segundo resumo.");
        assertEquals(200, api.post(summaryPath(team, card), ana.token(), null).status());
        assertEquals("Segundo resumo.", api.get(summaryPath(team, card), rui.token()).text("summary"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM card_summaries WHERE kanban_item_id = ?", Integer.class, card));
    }

    @Test
    @DisplayName("IT-SUM-03 Fewer than 100 words, or no script: 400 Dados insuficientes")
    void insufficientData() {
        Team team = newTeam();
        long noScript = projectId(team, "Sem roteiro");
        long shortScript = projectId(team, "Roteiro curto");
        long exact = projectId(team, "Cem palavras");
        saveScript(shortScript, "<p>" + words(99) + "</p>");
        saveScript(exact, "<p>" + words(100) + "</p>");

        for (long card : new long[]{noScript, shortScript}) {
            Resp r = api.post(summaryPath(team, card), ana.token(), null);
            assertEquals(400, r.status(), r.body());
            assertEquals("Dados insuficientes para gerar o resumo: escreva pelo menos 100 palavras no roteiro.", r.message());
        }
        assertNull(LAST_TEXT.get(), "the generator is not called with too little text");
        assertEquals(200, api.post(summaryPath(team, exact), ana.token(), null).status());
    }

    @Test
    @DisplayName("IT-SUM-04 Permissions: ADMIN and CREATOR generate; REVIEWER and outsider 403; anonymous 401; idea 400")
    void permissions() {
        Team team = newTeam();
        long card = projectId(team, "Permissões");
        saveScript(card, "<p>" + words(120) + "</p>");

        assertEquals(200, api.post(summaryPath(team, card), carlos.token(), null).status());
        assertEquals(403, api.post(summaryPath(team, card), rui.token(), null).status());
        assertEquals(403, api.post(summaryPath(team, card), bia.token(), null).status());
        assertEquals(401, api.post(summaryPath(team, card), null, null).status());
        assertEquals(404, api.post(summaryPath(team, 999999999L), ana.token(), null).status());

        long idea = ideaId(team, "Ideia");
        Resp r = api.post(summaryPath(team, idea), ana.token(), null);
        assertEquals(400, r.status(), r.body());
        assertEquals("Ideias não possuem roteiro para resumir.", r.message());
    }

    @Test
    @DisplayName("IT-SUM-05 Generator down: 503 and the previous summary is kept")
    void generatorDownKeepsOldSummary() {
        Team team = newTeam();
        long card = projectId(team, "Instável");
        saveScript(card, "<p>" + words(130) + "</p>");
        assertEquals(200, api.post(summaryPath(team, card), ana.token(), null).status());
        String before = api.get(summaryPath(team, card), ana.token()).body();

        DOWN.set(true);
        NEXT_SUMMARY.set("Não deve ser gravado.");
        Resp r = api.post(summaryPath(team, card), ana.token(), null);
        assertEquals(503, r.status(), r.body());
        assertEquals("Não foi possível gerar o resumo agora. Tente novamente.", r.message());

        assertEquals(before, api.get(summaryPath(team, card), ana.token()).body());
    }

    @Test
    @DisplayName("IT-SUM-06 Generator down with no summary yet: 503, then GET is still 404; deleting the card removes the summary")
    void downWithoutSummaryAndCascade() {
        Team team = newTeam();
        long card = projectId(team, "Nunca resumido");
        saveScript(card, "<p>" + words(130) + "</p>");

        DOWN.set(true);
        assertEquals(503, api.post(summaryPath(team, card), ana.token(), null).status());
        assertEquals(404, api.get(summaryPath(team, card), ana.token()).status());

        DOWN.set(false);
        assertEquals(200, api.post(summaryPath(team, card), ana.token(), null).status());
        assertEquals(204, api.delete(team.path() + "/cards/" + card, ana.token()).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM card_summaries WHERE kanban_item_id = ?", Integer.class, card));
    }
}
