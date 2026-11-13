package br.edu.ifsp.tcc.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/** Calls the Python summary service: POST {url}/summarize {"text"} returns {"summary"}. */
@Service
public class HttpSummaryGenerator implements SummaryGenerator {

    static final int MAX_WORDS = 20_000;
    static final String UNAVAILABLE = "Não foi possível gerar o resumo agora. Tente novamente.";

    private final RestClient restClient;

    @Autowired
    public HttpSummaryGenerator(@Value("${app.summary.url:http://localhost:8000}") String baseUrl) {
        this(baseUrl, Duration.ofSeconds(3), Duration.ofSeconds(30));
    }

    HttpSummaryGenerator(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public String summarize(String plainText) {
        String summary;
        try {
            SummaryReply reply = restClient.post().uri("/summarize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", capWords(plainText)))
                    .retrieve()
                    .body(SummaryReply.class);
            summary = reply == null ? null : reply.summary();
        } catch (Exception e) {
            throw unavailable(e);
        }
        if (summary == null || summary.isBlank()) throw unavailable(null);
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

    private static ResponseStatusException unavailable(Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE, cause);
    }

    record SummaryReply(String summary) { }
}
