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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** The YouTube Data API is replaced by a tiny embedded HTTP server; the real API is never called. */
class HttpYouTubeClientTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> receivedMethod = new AtomicReference<>();
    private final AtomicReference<String> receivedPath = new AtomicReference<>();
    private final AtomicReference<String> receivedQuery = new AtomicReference<>();
    private final AtomicReference<String> receivedKeyHeader = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/youtube/v3";
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void reply(int status, String body, long delayMs) {
        server.createContext("/youtube/v3/videos", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedPath.set(exchange.getRequestURI().getPath());
            receivedQuery.set(exchange.getRequestURI().getRawQuery());
            receivedKeyHeader.set(exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
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

    private HttpYouTubeClient client(Duration read) {
        return new HttpYouTubeClient(baseUrl, "secret-key", Duration.ofSeconds(3), read);
    }

    private void assertUnavailable(Runnable call) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, call::run);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals("Não foi possível consultar o YouTube agora. Tente novamente.", ex.getReason());
        assertFalse(String.valueOf(ex.getMessage()).contains("secret-key"));
    }

    @Test
    void fetch_shouldCallVideosEndpointAndMapStatistics() {
        reply(200, """
                {"items":[{"id":"dQw4w9WgXcQ","snippet":{"title":"Aula 1","publishedAt":"2024-03-05T10:20:30Z"},
                "statistics":{"viewCount":"1234","likeCount":"56","commentCount":"7"}}]}""", 0);

        Map<String, VideoStats> result = client(Duration.ofSeconds(5)).fetch(List.of("dQw4w9WgXcQ"));

        assertEquals("GET", receivedMethod.get());
        assertEquals("/youtube/v3/videos", receivedPath.get());
        assertTrue(receivedQuery.get().contains("part=statistics,snippet") || receivedQuery.get().contains("part=statistics%2Csnippet"));
        assertTrue(receivedQuery.get().contains("id=dQw4w9WgXcQ"));
        assertEquals("secret-key", receivedKeyHeader.get());
        assertFalse(receivedQuery.get().contains("key="), "the key travels in a header, never in the URL");
        assertFalse(receivedQuery.get().contains("secret-key"));
        assertTrue(receivedQuery.get().contains("fields=items"));
        assertTrue(receivedQuery.get().contains("viewCount,likeCount,commentCount") || receivedQuery.get().contains("viewCount%2ClikeCount%2CcommentCount"));
        VideoStats stats = result.get("dQw4w9WgXcQ");
        assertEquals(1234L, stats.viewCount());
        assertEquals(56L, stats.likeCount());
        assertEquals(7L, stats.commentCount());
        assertEquals("Aula 1", stats.title());
        assertEquals(LocalDateTime.of(2024, 3, 5, 10, 20, 30), stats.publishedAt());
    }

    @Test
    void fetch_shouldJoinSeveralIdsWithCommas() {
        reply(200, "{\"items\":[]}", 0);

        client(Duration.ofSeconds(5)).fetch(List.of("aaaaaaaaaaa", "bbbbbbbbbbb"));

        assertTrue(receivedQuery.get().contains("id=aaaaaaaaaaa,bbbbbbbbbbb") || receivedQuery.get().contains("id=aaaaaaaaaaa%2Cbbbbbbbbbbb"));
    }

    @Test
    void fetch_shouldStoreNullWhenLikeAndCommentCountsAreHidden() {
        reply(200, """
                {"items":[{"id":"dQw4w9WgXcQ","snippet":{"title":"Sem likes"},"statistics":{"viewCount":"10"}}]}""", 0);

        VideoStats stats = client(Duration.ofSeconds(5)).fetch(List.of("dQw4w9WgXcQ")).get("dQw4w9WgXcQ");

        assertEquals(10L, stats.viewCount());
        assertNull(stats.likeCount());
        assertNull(stats.commentCount());
        assertNull(stats.publishedAt());
    }

    @Test
    void fetch_shouldTolerateMissingSectionsAndUnreadableValues() {
        reply(200, """
                {"items":[{"id":"aaaaaaaaaaa"},
                {"id":"bbbbbbbbbbb","snippet":{"title":"  ","publishedAt":"not-a-date"},"statistics":{"viewCount":"x","likeCount":"y"}},
                {"snippet":{}}, "junk"]}""", 0);

        Map<String, VideoStats> result = client(Duration.ofSeconds(5)).fetch(List.of("aaaaaaaaaaa", "bbbbbbbbbbb"));

        assertEquals(2, result.size());
        assertEquals(0L, result.get("aaaaaaaaaaa").viewCount());
        assertNull(result.get("bbbbbbbbbbb").title());
        assertNull(result.get("bbbbbbbbbbb").publishedAt());
        assertNull(result.get("bbbbbbbbbbb").likeCount());
    }

    @Test
    void fetch_shouldReturnEmptyMapWhenItemsIsEmptyOrMissing() {
        reply(200, "{\"items\":[]}", 0);
        assertTrue(client(Duration.ofSeconds(5)).fetch(List.of("dQw4w9WgXcQ")).isEmpty());
    }

    @Test
    void fetch_shouldReturnEmptyMapWhenReplyHasNoItems() {
        reply(200, "{\"kind\":\"youtube#videoListResponse\"}", 0);
        assertTrue(client(Duration.ofSeconds(5)).fetch(List.of("dQw4w9WgXcQ")).isEmpty());
    }

    @Test
    void fetch_shouldFailWith503OnQuotaError() {
        reply(403, "{\"error\":{\"code\":403,\"errors\":[{\"reason\":\"quotaExceeded\"}]}}", 0);

        assertUnavailable(() -> client(Duration.ofSeconds(5)).fetch(List.of("dQw4w9WgXcQ")));
    }

    @Test
    void fetch_shouldFailWith503OnServerError() {
        reply(500, "{}", 0);

        assertUnavailable(() -> client(Duration.ofSeconds(5)).fetch(List.of("dQw4w9WgXcQ")));
    }

    @Test
    void fetch_shouldFailWith503OnTimeout() {
        reply(200, "{\"items\":[]}", 1500);

        assertUnavailable(() -> client(Duration.ofMillis(300)).fetch(List.of("dQw4w9WgXcQ")));
    }

    @Test
    void fetch_shouldFailWith503WhenServerIsDown() {
        server.stop(0);

        assertUnavailable(() -> client(Duration.ofSeconds(2)).fetch(List.of("dQw4w9WgXcQ")));
    }

    @Test
    void fetch_shouldFailWith503WithoutCallingWhenNoKeyIsConfigured() {
        reply(200, "{\"items\":[]}", 0);
        HttpYouTubeClient noKey = new HttpYouTubeClient(baseUrl, "  ", Duration.ofSeconds(3), Duration.ofSeconds(5));

        assertFalse(noKey.isConfigured());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> noKey.fetch(List.of("dQw4w9WgXcQ")));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals("Integração com o YouTube não configurada.", ex.getReason());
        assertNull(receivedMethod.get());
    }

    @Test
    void isConfigured_shouldBeFalseForNullKeyAndTrueForAKey() {
        assertFalse(new HttpYouTubeClient(baseUrl, null, Duration.ofSeconds(1), Duration.ofSeconds(1)).isConfigured());
        assertTrue(client(Duration.ofSeconds(1)).isConfigured());
        assertTrue(new HttpYouTubeClient(baseUrl, "k").isConfigured());
    }

    @Test
    void parse_shouldReturnEmptyForNullReply() {
        assertTrue(HttpYouTubeClient.parse(null).isEmpty());
    }
}
