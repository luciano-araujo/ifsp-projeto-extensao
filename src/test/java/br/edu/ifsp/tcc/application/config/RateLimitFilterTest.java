package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private static final String PROXIES = "127.0.0.1,::1";

    @Mock private RateLimitService rateLimitService;
    @Mock private FilterChain chain;

    private RateLimitFilter filter(boolean enabled) {
        return new RateLimitFilter(rateLimitService, enabled, 10, 100, 10, PROXIES);
    }

    private MockHttpServletRequest post(String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("203.0.113.5");
        request.setServletPath(path);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    @Test
    void shouldPassThroughAndReplayBodyWhenUnderLimit() throws Exception {
        when(rateLimitService.registerAttempt("ip|/api/auth/login|203.0.113.5", 100)).thenReturn(0L);
        when(rateLimitService.registerAttempt("/api/auth/login|203.0.113.5|ana@x.com", 10)).thenReturn(0L);
        MockHttpServletRequest request = post("/api/auth/login", "{\"email\":\" Ana@X.com \",\"password\":\"p\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(true).doFilter(request, response, chain);

        ArgumentCaptor<HttpServletRequest> forwarded = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(chain).doFilter(forwarded.capture(), eq(response));
        String expected = "{\"email\":\" Ana@X.com \",\"password\":\"p\"}";
        assertEquals(expected, new String(forwarded.getValue().getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertEquals(expected, forwarded.getValue().getReader().readLine());
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAnswer429WithRetryAfterWhenOverLimit() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L, 321L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(true).doFilter(post("/api/auth/password/forgot", "{\"email\":\"a@b.com\"}"), response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("321", response.getHeader("Retry-After"));
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("{\"message\":\"Muitas tentativas. Aguarde alguns minutos e tente novamente.\"}",
                response.getContentAsString(StandardCharsets.UTF_8));
        verifyNoInteractions(chain);
    }

    @Test
    void shouldKeyByEmptyEmailWhenBodyIsNotJsonOrHasNoEmail() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L);

        filter(true).doFilter(post("/api/auth/login", "not json"), new MockHttpServletResponse(), chain);
        filter(true).doFilter(post("/api/auth/login", "{\"email\":5}"), new MockHttpServletResponse(), chain);
        filter(true).doFilter(post("/api/auth/login", ""), new MockHttpServletResponse(), chain);

        verify(rateLimitService, times(3)).registerAttempt("/api/auth/login|203.0.113.5|", 10);
        verify(chain, times(3)).doFilter(any(), any());
    }

    @Test
    void shouldTrustForwardedForOnlyFromLocalProxy() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L);

        MockHttpServletRequest viaNginx = post("/api/auth/login", "{\"email\":\"a@b.com\"}");
        viaNginx.setRemoteAddr("127.0.0.1");
        viaNginx.addHeader("X-Forwarded-For", "6.6.6.6, 198.51.100.7");
        filter(true).doFilter(viaNginx, new MockHttpServletResponse(), chain);
        verify(rateLimitService).registerAttempt("/api/auth/login|198.51.100.7|a@b.com", 10);

        MockHttpServletRequest spoofed = post("/api/auth/login", "{\"email\":\"a@b.com\"}");
        spoofed.addHeader("X-Forwarded-For", "6.6.6.6");
        filter(true).doFilter(spoofed, new MockHttpServletResponse(), chain);
        verify(rateLimitService).registerAttempt("/api/auth/login|203.0.113.5|a@b.com", 10);

        MockHttpServletRequest noHeader = post("/api/auth/login", "{\"email\":\"a@b.com\"}");
        noHeader.setRemoteAddr("127.0.0.1");
        filter(true).doFilter(noHeader, new MockHttpServletResponse(), chain);
        verify(rateLimitService).registerAttempt("/api/auth/login|127.0.0.1|a@b.com", 10);
    }

    @Test
    void shouldIgnoreOtherPathsMethodsAndDisabledFilter() throws Exception {
        filter(true).doFilter(post("/api/v1/teams", "{}"), new MockHttpServletResponse(), chain);
        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/auth/login");
        filter(true).doFilter(get, new MockHttpServletResponse(), chain);
        filter(false).doFilter(post("/api/auth/login", "{}"), new MockHttpServletResponse(), chain);

        verify(chain, times(3)).doFilter(any(), any());
        verifyNoInteractions(rateLimitService);
    }

    @Test
    void shouldMatchTheDecodedPathSoEncodedLoginIsLimitedToo() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L);
        MockHttpServletRequest request = post("/api/auth/login", "{\"email\":\"a@b.com\"}");
        request.setRequestURI("/api/auth/%6Cogin");   // raw URI is encoded, the servlet path is decoded

        filter(true).doFilter(request, new MockHttpServletResponse(), chain);

        verify(rateLimitService).registerAttempt("/api/auth/login|203.0.113.5|a@b.com", 10);
        verify(rateLimitService).registerAttempt("ip|/api/auth/login|203.0.113.5", 100);
    }

    @Test
    void shouldBlockWithoutChargingTheEmailBucketWhenThePerIpBucketIsExceeded() throws Exception {
        when(rateLimitService.registerAttempt("ip|/api/auth/password/forgot|203.0.113.5", 100)).thenReturn(77L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(true).doFilter(post("/api/auth/password/forgot", "{\"email\":\"new@x.com\"}"), response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("77", response.getHeader("Retry-After"));
        verify(rateLimitService, never()).registerAttempt(eq("/api/auth/password/forgot|203.0.113.5|new@x.com"), anyInt());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldAnswer413AboveBodyCapWithoutCountingTheAttempt() throws Exception {
        String big = "{\"email\":\"" + "a".repeat(RateLimitFilter.MAX_BODY_BYTES) + "\"}";
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(true).doFilter(post("/api/auth/login", big), response, chain);

        assertEquals(413, response.getStatus());
        assertEquals("{\"message\":\"Requisição muito grande.\"}", response.getContentAsString(StandardCharsets.UTF_8));
        verifyNoInteractions(chain, rateLimitService);
    }

    @Test
    void shouldAcceptBodyExactlyAtTheCap() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L);
        String json = "{\"p\":\"";
        String body = json + "a".repeat(RateLimitFilter.MAX_BODY_BYTES - json.length() - 2) + "\"}";
        assertEquals(RateLimitFilter.MAX_BODY_BYTES, body.length());

        filter(true).doFilter(post("/api/auth/login", body), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void shouldCapTheEmailUsedInTheKey() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L);
        String longEmail = "x".repeat(300) + "@b.com";

        filter(true).doFilter(post("/api/auth/login", "{\"email\":\"" + longEmail + "\"}"),
                new MockHttpServletResponse(), chain);

        verify(rateLimitService).registerAttempt("/api/auth/login|203.0.113.5|" + "x".repeat(254), 10);
    }

    @Test
    void shouldFallBackToRemoteAddressWhenForwardedForHasNoUsableHop() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L);
        for (String header : new String[]{",", " , ", "  "}) {
            MockHttpServletRequest request = post("/api/auth/login", "{\"email\":\"a@b.com\"}");
            request.setRemoteAddr("127.0.0.1");
            request.addHeader("X-Forwarded-For", header);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter(true).doFilter(request, response, chain);

            assertEquals(200, response.getStatus());
        }
        verify(rateLimitService, times(3)).registerAttempt("/api/auth/login|127.0.0.1|a@b.com", 10);
    }

    @Test
    void shouldLimitAllFiveAuthRoutes() {
        assertEquals(5, RateLimitFilter.LIMITED_PATHS.size());
        assertTrue(RateLimitFilter.LIMITED_PATHS.contains("/api/auth/register/confirm"));
        assertTrue(RateLimitFilter.LIMITED_PATHS.contains("/api/auth/register/request-token"));
        assertTrue(RateLimitFilter.LIMITED_PATHS.contains("/api/auth/password/reset"));
    }

    private MockHttpServletRequest user(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("203.0.113.5");
        request.setServletPath(path);
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        return request;
    }

    @Test
    void shouldLimitThePasswordAndEmailChangeRoutesPerUserAndIp() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L);

        filter(true).doFilter(user("PUT", "/api/v1/users/7/password"), new MockHttpServletResponse(), chain);
        filter(true).doFilter(user("POST", "/api/v1/users/7/email-change"), new MockHttpServletResponse(), chain);

        verify(rateLimitService).registerAttempt("user|/api/v1/users/7/password|203.0.113.5", 10);
        verify(rateLimitService).registerAttempt("ip|/api/v1/users/{id}/password|203.0.113.5", 100);
        verify(rateLimitService).registerAttempt("user|/api/v1/users/7/email-change|203.0.113.5", 10);
        verify(rateLimitService).registerAttempt("ip|/api/v1/users/{id}/email-change|203.0.113.5", 100);
        verify(chain, times(2)).doFilter(any(), any());
    }

    @Test
    void shouldAnswer429ForTheUserRoutesWithTheSameBodyAsTheAuthRoutes() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L, 45L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter(true).doFilter(user("PUT", "/api/v1/users/7/password"), response, chain);

        assertEquals(429, response.getStatus());
        assertEquals("45", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains(RateLimitFilter.MESSAGE));
        verifyNoInteractions(chain);
    }

    @Test
    void shouldNotLimitTheEmailConfirmationOrOtherMethodsOrOtherUserRoutes() throws Exception {
        filter(true).doFilter(user("POST", "/api/v1/users/7/email-change/confirm"), new MockHttpServletResponse(), chain);
        filter(true).doFilter(user("GET", "/api/v1/users/7/password"), new MockHttpServletResponse(), chain);
        filter(true).doFilter(user("POST", "/api/v1/users/7/password"), new MockHttpServletResponse(), chain);
        filter(true).doFilter(user("PUT", "/api/v1/users/7"), new MockHttpServletResponse(), chain);
        filter(true).doFilter(user("PUT", "/api/v1/users/abc/password"), new MockHttpServletResponse(), chain);

        verify(chain, times(5)).doFilter(any(), any());
        verifyNoInteractions(rateLimitService);
    }

    @Test
    void shouldKeyTheUserBucketByTheParsedIdSoLeadingZerosShareTheBucket() throws Exception {
        when(rateLimitService.registerAttempt(anyString(), anyInt())).thenReturn(0L);

        filter(true).doFilter(user("PUT", "/api/v1/users/007/password"), new MockHttpServletResponse(), chain);
        filter(true).doFilter(user("PUT", "/api/v1/users/7/password"), new MockHttpServletResponse(), chain);

        verify(rateLimitService, times(2)).registerAttempt("user|/api/v1/users/7/password|203.0.113.5", 10);
    }
}
