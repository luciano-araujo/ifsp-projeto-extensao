package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.service.RateLimitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Limits attempts on the public auth routes per route + client IP + e-mail (BE-44).
 * Answers 429 with Retry-After once the limit is exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register/request-token",
            "/api/auth/register/confirm",
            "/api/auth/password/forgot",
            "/api/auth/password/reset");

    static final String MESSAGE = "Muitas tentativas. Aguarde alguns minutos e tente novamente.";

    private final RateLimitService rateLimitService;
    private final boolean enabled;
    private final Set<String> trustedProxies;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitService rateLimitService,
                           @Value("${app.rate-limit.enabled:true}") boolean enabled,
                           @Value("${app.rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1,::1}") String trustedProxies) {
        this.rateLimitService = rateLimitService;
        this.enabled = enabled;
        this.trustedProxies = Set.copyOf(Arrays.stream(trustedProxies.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equalsIgnoreCase(request.getMethod())
                || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        String key = request.getRequestURI() + "|" + clientIp(request) + "|" + extractEmail(body);

        long retryAfter = rateLimitService.registerAttempt(key);
        if (retryAfter > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getOutputStream(), Map.of("message", MESSAGE));
            return;
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    /** X-Forwarded-For is honoured only when the direct peer is a trusted proxy (local nginx). */
    String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (remote != null && trustedProxies.contains(remote) && forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            // the last hop is the one appended by our own proxy; earlier ones can be forged by the client
            return hops[hops.length - 1].trim();
        }
        return remote == null ? "unknown" : remote;
    }

    private String extractEmail(byte[] body) {
        try {
            JsonNode email = objectMapper.readTree(body).path("email");
            return email.isTextual() ? email.asText().trim().toLowerCase(Locale.ROOT) : "";
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    /** Lets the controller read the body that the filter already consumed. */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return source.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { /* synchronous only */ }
                @Override public int read() { return source.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding() == null ? StandardCharsets.UTF_8.name() : getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.Charset.forName(encoding)));
        }
    }
}
