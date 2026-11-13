package br.edu.ifsp.tcc.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/** Calls the Python summary service: POST {url}/summarize {"text"} returns {"summary"}. */
@Service
public class HttpSummaryGenerator implements SummaryGenerator {

    static final int MAX_WORDS = 20_000;
    static final String UNAVAILABLE = "Não foi possível gerar o resumo agora. Tente novamente.";

    private static final Logger log = LoggerFactory.getLogger(HttpSummaryGenerator.class);

    private final RestClient restClient;

    @Autowired
    public HttpSummaryGenerator(@Value("${app.summary.url:http://localhost:8000}") String baseUrl) {
        this(baseUrl, Duration.ofSeconds(3), Duration.ofSeconds(30));
    }

    HttpSummaryGenerator(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public String summarize(String plainText) {
        String summary;
        try {
            SummaryReply reply = restClient.post().uri("/summarize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", escapeMarkup(capWords(plainText))))
                    .retrieve()
                    .body(SummaryReply.class);
            summary = reply == null ? null : reply.summary();
        } catch (RestClientResponseException e) {
            log.warn("Summary service answered HTTP {}", e.getStatusCode().value());
            throw unavailable(e);
        } catch (Exception e) {
            log.warn("Summary service call failed: {}", e.getClass().getSimpleName());
            throw unavailable(e);
        }
        if (summary == null || summary.isBlank()) {
            log.warn("Summary service returned an empty summary");
            throw unavailable(null);
        }
        return summary.trim();
    }

    /** Keeps only the first {@value #MAX_WORDS} words. */
    static String capWords(String text) {
        int words = 0;
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            boolean space = Character.isWhitespace(text.charAt(i));
            if (!space && !inWord) {
                if (++words > MAX_WORDS) return text.substring(0, i).trim();
            }
            inWord = !space;
        }
        return text;
    }

    /**
     * The plain text has entities decoded; the Python side strips tags and then unescapes, so a literal
     * "<b>" typed by the author must travel as "&lt;b&gt;" to survive and count as words.
     */
    static String escapeMarkup(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static ResponseStatusException unavailable(Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE, cause);
    }

    record SummaryReply(String summary) { }
}
