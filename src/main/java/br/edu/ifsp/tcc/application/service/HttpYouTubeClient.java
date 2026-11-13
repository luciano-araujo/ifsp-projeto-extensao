package br.edu.ifsp.tcc.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls GET {base}/videos?part=statistics,snippet&id=a,b&key=... of the YouTube Data API v3.
 * The key and the URL (which carries the key) are never logged and never put in an exception.
 */
@Service
public class HttpYouTubeClient implements YouTubeClient {

    static final String UNAVAILABLE = "Não foi possível consultar o YouTube agora. Tente novamente.";
    static final String NOT_CONFIGURED = "Integração com o YouTube não configurada.";

    private static final Logger log = LoggerFactory.getLogger(HttpYouTubeClient.class);

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
    public HttpYouTubeClient(@Value("${app.youtube.base-url:https://www.googleapis.com/youtube/v3}") String baseUrl,
                             @Value("${app.youtube.api-key:}") String apiKey) {
        this(baseUrl, apiKey, Duration.ofSeconds(3), Duration.ofSeconds(10));
    }

    HttpYouTubeClient(String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public Map<String, VideoStats> fetch(List<String> videoIds) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, NOT_CONFIGURED);
        }
        Map<String, Object> reply;
        try {
            reply = restClient.get()
                    .uri(uri -> uri.path("/videos").queryParam("part", "statistics,snippet")
                            .queryParam("id", String.join(",", videoIds)).queryParam("key", apiKey).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        } catch (RestClientResponseException e) {
            log.warn("YouTube answered HTTP {}", e.getStatusCode().value());
            throw unavailable();
        } catch (Exception e) {
            // Only the class: the message of a transport error carries the request URL, and so the key
            log.warn("YouTube call failed: {}", e.getClass().getSimpleName());
            throw unavailable();
        }
        return parse(reply);
    }

    static Map<String, VideoStats> parse(Map<String, Object> reply) {
        Map<String, VideoStats> result = new HashMap<>();
        if (reply == null || !(reply.get("items") instanceof List<?> items)) return result;
        for (Object raw : items) {
            if (!(raw instanceof Map<?, ?> item) || !(item.get("id") instanceof String id)) continue;
            Map<?, ?> statistics = item.get("statistics") instanceof Map<?, ?> s ? s : Map.of();
            Map<?, ?> snippet = item.get("snippet") instanceof Map<?, ?> s ? s : Map.of();
            Long views = number(statistics.get("viewCount"));
            result.put(id, new VideoStats(id, views == null ? 0L : views, number(statistics.get("likeCount")),
                    number(statistics.get("commentCount")), text(snippet.get("title")), instant(snippet.get("publishedAt"))));
        }
        return result;
    }

    /** The statistics come as JSON strings; absent or unreadable values become null. */
    private static Long number(Object value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(Object value) {
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    private static LocalDateTime instant(Object value) {
        if (!(value instanceof String s)) return null;
        try {
            return LocalDateTime.ofInstant(Instant.parse(s), ZoneOffset.UTC);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }
}
