package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * QA of M2, summary (contract 7.11) against a service that is NOT a stub.
 * Needs the Python service of automation/ running without ANTHROPIC_API_KEY (extractive engine) on localhost:8000;
 * the tests are skipped when nothing listens there. No real LLM is ever called.
 */
class QaM2SummaryServiceIT extends AbstractIT {

    private static boolean pythonUp() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 8000), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    @DisplayName("IT-SUM-E2E Real call through the Python service (extractive): 200, summary saved and readable")
    void realServiceReturnsSummary() {
        assumeTrue(pythonUp(), "Python service not running on localhost:8000");
        Team team = newTeam();
        long card = projectId(team, "Fotossintese");
        String sentence = "A fotossintese converte luz solar em energia quimica nas folhas das plantas. ";
        String html = "<h1>Fotossintese</h1><p>" + String.join("", Collections.nCopies(12, sentence)) + "</p>";
        assertEquals(200, api.put("/api/v1/kanban/" + card + "/script", ana.token(), map("content", html)).status());

        String path = team.path() + "/cards/" + card + "/summary";
        assertEquals(404, api.get(path, rui.token()).status());
        Resp generated = api.post(path, carlos.token(), null);
        assertEquals(200, generated.status(), generated.body());
        assertEquals("SCRIPT", generated.text("basedOn"));
        assertEquals(card, generated.num("cardId"));
        assertFalse(generated.text("summary").isBlank());
        assertNotNull(generated.text("generatedAt"));

        Resp read = api.get(path, rui.token());
        assertEquals(200, read.status());
        assertEquals(generated.text("summary"), read.text("summary"));
    }

    @Test
    @DisplayName("IT-SUM-E2E-2 Markup typed in the script survives the round trip as text and does not break the service")
    void markupInScript() {
        assumeTrue(pythonUp(), "Python service not running on localhost:8000");
        Team team = newTeam();
        long card = projectId(team, "Markup");
        String sentence = "Use a tag &lt;b&gt;negrito&lt;/b&gt; no texto do roteiro com cuidado. ";
        assertEquals(200, api.put("/api/v1/kanban/" + card + "/script", ana.token(),
                map("content", "<p>" + String.join("", Collections.nCopies(12, sentence)) + "</p>")).status());
        Resp r = api.post(team.path() + "/cards/" + card + "/summary", ana.token(), null);
        assertEquals(200, r.status(), r.body());
        assertFalse(r.text("summary").isBlank());
    }
}
