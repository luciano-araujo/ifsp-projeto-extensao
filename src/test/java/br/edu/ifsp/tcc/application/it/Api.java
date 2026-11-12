package br.edu.ifsp.tcc.application.it;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal JDK HTTP client pointed at the Spring Boot server started by the test (real HTTP, real filters). */
public class Api {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;

    public Api(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    public Resp get(String path, String token) { return send("GET", path, token, null); }

    public Resp post(String path, String token, Object body) { return send("POST", path, token, body); }

    public Resp put(String path, String token, Object body) { return send("PUT", path, token, body); }

    public Resp patch(String path, String token, Object body) { return send("PATCH", path, token, body); }

    public Resp delete(String path, String token) { return send("DELETE", path, token, null); }

    /** body: null (no body), a String (sent raw as JSON text) or any object (serialized to JSON). */
    public Resp send(String method, String path, String token, Object body) {
        return send(method, path, token, body, Map.of());
    }

    public Resp send(String method, String path, String token, Object body, Map<String, String> extraHeaders) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30));
            if (token != null) builder.header("Authorization", "Bearer " + token);
            extraHeaders.forEach(builder::header);
            HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.noBody();
            if (body != null) {
                String json = body instanceof String s ? s : Resp.mapper().writeValueAsString(body);
                publisher = HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
                builder.header("Content-Type", "application/json; charset=utf-8");
            }
            HttpResponse<String> response = client.send(builder.method(method, publisher).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) map.put((String) keyValues[i], keyValues[i + 1]);
        return map;
    }
}
