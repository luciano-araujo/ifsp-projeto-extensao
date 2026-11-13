package br.edu.ifsp.tcc.application.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** The Python service is replaced by a tiny embedded HTTP server; no real LLM is ever called. */
class HttpSummaryGeneratorTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedPath = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void reply(int status, String body, long delayMs) {
        server.createContext("/summarize", exchange -> {
            receivedPath.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            try {
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (IOException ignored) {
                // the client already gave up (timeout test)
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    private HttpSummaryGenerator generator(Duration read) {
        return new HttpSummaryGenerator(baseUrl, Duration.ofSeconds(3), read);
    }

    private void assertUnavailable(Runnable call) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, call::run);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals("Não foi possível gerar o resumo agora. Tente novamente.", ex.getReason());
    }

    @Test
    void summarize_shouldPostTextAndReturnTrimmedSummary() {
        reply(200, "{\"summary\":\"  Resumo do roteiro.  \"}", 0);

        String summary = generator(Duration.ofSeconds(5)).summarize("texto do roteiro");

        assertEquals("Resumo do roteiro.", summary);
        assertEquals("POST /summarize", receivedPath.get());
        assertEquals("{\"text\":\"texto do roteiro\"}", receivedBody.get());
    }

    @Test
    void summarize_shouldIgnoreUnknownFieldsOfTheReply() {
        reply(200, "{\"summary\":\"ok\",\"engine\":\"extractive\"}", 0);

        assertEquals("ok", generator(Duration.ofSeconds(5)).summarize("texto"));
    }

    @Test
    void summarize_shouldSendOnlyTheFirst20000Words() {
        reply(200, "{\"summary\":\"ok\"}", 0);
        String huge = String.join(" ", Collections.nCopies(25_000, "a"));

        generator(Duration.ofSeconds(5)).summarize(huge);

        String text = receivedBody.get();
        int words = text.substring("{\"text\":\"".length(), text.length() - 2).split(" ").length;
        assertEquals(20_000, words);
    }

    @Test
    void summarize_shouldFailWith503OnServerError() {
        reply(500, "{\"detail\":\"boom\"}", 0);

        assertUnavailable(() -> generator(Duration.ofSeconds(5)).summarize("texto"));
    }

    @Test
    void summarize_shouldFailWith503OnUnprocessableEntity() {
        reply(422, "{\"detail\":\"texto curto\"}", 0);

        assertUnavailable(() -> generator(Duration.ofSeconds(5)).summarize("texto"));
    }

    @Test
    void summarize_shouldFailWith503OnEmptySummary() {
        reply(200, "{\"summary\":\"   \"}", 0);

        assertUnavailable(() -> generator(Duration.ofSeconds(5)).summarize("texto"));
    }

    @Test
    void summarize_shouldFailWith503OnMissingSummaryField() {
        reply(200, "{}", 0);

        assertUnavailable(() -> generator(Duration.ofSeconds(5)).summarize("texto"));
    }

    @Test
    void summarize_shouldFailWith503OnEmptyBody() {
        reply(200, "", 0);

        assertUnavailable(() -> generator(Duration.ofSeconds(5)).summarize("texto"));
    }

    @Test
    void summarize_shouldFailWith503OnReadTimeout() {
        reply(200, "{\"summary\":\"tarde demais\"}", 1500);

        assertUnavailable(() -> generator(Duration.ofMillis(300)).summarize("texto"));
    }

    @Test
    void summarize_shouldFailWith503WhenServiceIsDown() {
        server.stop(0);

        assertUnavailable(() -> generator(Duration.ofSeconds(5)).summarize("texto"));
    }

    @Test
    void capWords_shouldKeepShortTextUntouched() {
        assertEquals("poucas palavras", HttpSummaryGenerator.capWords("poucas palavras"));
    }

    @Test
    void defaultConstructor_shouldBuildWithConfiguredUrl() {
        assertNotNull(new HttpSummaryGenerator("http://localhost:8000"));
    }
}
