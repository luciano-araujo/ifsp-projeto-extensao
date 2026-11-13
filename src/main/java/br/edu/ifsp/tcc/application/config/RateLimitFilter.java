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
import org.springframework.core.annotation.Order;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Limits attempts on the public auth routes per route + client IP + e-mail (BE-44).
 * Answers 429 with Retry-After once the limit is exceeded.
 */
@Component
// Order 0 runs after the Spring Security chain (-100), so the CORS headers are already on the response
// when a 429 or 413 is written here: the browser can read the error and Retry-After.
@Order(0)
public class RateLimitFilter extends OncePerRequestFilter {

    static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register/request-token",
            "/api/auth/register/confirm",
            "/api/auth/password/forgot",
            "/api/auth/password/reset");

    static final Pattern USER_ROUTE = Pattern.compile("^/api/v1/users/(\\d{1,18})/(password|email-change)$");

    static final int MAX_BODY_BYTES = 16 * 1024;
    static final int MAX_EMAIL_LENGTH = 254;
    static final String TOO_LARGE_MESSAGE = "Requisição muito grande.";
    static final String MESSAGE = "Muitas tentativas. Aguarde alguns minutos e tente novamente.";

    private final RateLimitService rateLimitService;
    private final boolean enabled;
    private final int maxAttempts;
    private final int maxAttemptsPerIp;
    private final int maxAttemptsUser;
    private final Set<String> trustedProxies;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitService rateLimitService,
                           @Value("${app.rate-limit.enabled:true}") boolean enabled,
                           @Value("${app.rate-limit.max-attempts:10}") int maxAttempts,
                           @Value("${app.rate-limit.max-attempts-per-ip:100}") int maxAttemptsPerIp,
                           @Value("${app.rate-limit.max-attempts-user:10}") int maxAttemptsUser,
                           @Value("${app.rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1,::1}") String trustedProxies) {
        this.rateLimitService = rateLimitService;
        this.enabled = enabled;
        this.maxAttempts = maxAttempts;
        this.maxAttemptsPerIp = maxAttemptsPerIp;
        this.maxAttemptsUser = maxAttemptsUser;
        this.trustedProxies = Set.copyOf(Arrays.stream(trustedProxies.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || (!isAuthRoute(request) && userRoute(request) == null);
    }

    private static boolean isAuthRoute(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && LIMITED_PATHS.contains(pathOf(request));
    }

    /**
     * Sensitive routes of a logged-in user (QA4-03): PUT .../password and POST .../email-change. Their attempts are counted
     * per user id (from the path) and client IP. The e-mail confirmation is not limited here: its code is discarded
     * after 5 wrong attempts. Returns the matcher, or null when the request is not one of these routes.
     */
    private static Matcher userRoute(HttpServletRequest request) {
        Matcher matcher = USER_ROUTE.matcher(pathOf(request));
        if (!matcher.matches()) return null;
        boolean right = "password".equals(matcher.group(2))
                ? "PUT".equalsIgnoreCase(request.getMethod())
                : "POST".equalsIgnoreCase(request.getMethod());
        return right ? matcher : null;
    }

    /** Decoded and normalized path (so /api/auth/%6Cogin is the login route), not the raw request URI. */
    private static String pathOf(HttpServletRequest request) {
        String servletPath = request.getServletPath() == null ? "" : request.getServletPath();
        String pathInfo = request.getPathInfo() == null ? "" : request.getPathInfo();
        return servletPath + pathInfo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            writeError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, TOO_LARGE_MESSAGE);
            return;
        }
        String path = pathOf(request);
        String ip = clientIp(request);

        Matcher userRoute = userRoute(request);
        long retryAfter;
        if (userRoute != null) {
            // per user (id from the path) + IP; the per-IP bucket is per route template, so other users do not share it
            String template = "/api/v1/users/{id}/" + userRoute.group(2);
            retryAfter = Math.max(
                    rateLimitService.registerAttempt("ip|" + template + "|" + ip, maxAttemptsPerIp),
                    rateLimitService.registerAttempt("user|" + path + "|" + ip, maxAttemptsUser));
        } else {
            // per route + IP (stops rotating e-mails) and per route + IP + e-mail
            retryAfter = Math.max(
                    rateLimitService.registerAttempt("ip|" + path + "|" + ip, maxAttemptsPerIp),
                    rateLimitService.registerAttempt(path + "|" + ip + "|" + extractEmail(body), maxAttempts));
        }
        if (retryAfter > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            writeError(response, 429, MESSAGE);
            return;
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), Map.of("message", message));
    }

    /** X-Forwarded-For is honoured only when the direct peer is a trusted proxy (local nginx). */
    String clientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (remote != null && trustedProxies.contains(remote) && forwarded != null && !forwarded.isBlank()) {
            // the last non-blank hop is the one appended by our own proxy; earlier ones can be forged
            String[] hops = forwarded.split(",");
            for (int i = hops.length - 1; i >= 0; i--) {
                if (!hops[i].isBlank()) return hops[i].trim();
            }
        }
        return remote == null ? "unknown" : remote;
    }

    private String extractEmail(byte[] body) {
        try {
            JsonNode email = objectMapper.readTree(body).path("email");
            if (!email.isTextual()) return "";
            String value = email.asText().trim().toLowerCase(Locale.ROOT);
            return value.length() > MAX_EMAIL_LENGTH ? value.substring(0, MAX_EMAIL_LENGTH) : value;
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
