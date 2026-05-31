package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.User;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-key-for-unit-tests", 3600000);
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("Test User");
        return user;
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtService.generateToken(createUser());
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_shouldReturnDecodedJwtForValidToken() {
        String token = jwtService.generateToken(createUser());
        DecodedJWT decoded = jwtService.validateToken(token);
        assertNotNull(decoded);
        assertEquals("1", decoded.getSubject());
        assertEquals("test@example.com", decoded.getClaim("email").asString());
        assertEquals("Test User", decoded.getClaim("name").asString());
        assertEquals("creator-workbench", decoded.getIssuer());
    }

    @Test
    void validateToken_shouldReturnNullForInvalidToken() {
        DecodedJWT decoded = jwtService.validateToken("invalid.token.here");
        assertNull(decoded);
    }

    @Test
    void validateToken_shouldReturnNullForTamperedToken() {
        String token = jwtService.generateToken(createUser());
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertNull(jwtService.validateToken(tampered));
    }

    @Test
    void getUserIdFromToken_shouldReturnUserIdForValidToken() {
        String token = jwtService.generateToken(createUser());
        Long userId = jwtService.getUserIdFromToken(token);
        assertEquals(1L, userId);
    }

    @Test
    void getUserIdFromToken_shouldReturnNullForInvalidToken() {
        assertNull(jwtService.getUserIdFromToken("bad-token"));
    }

    @Test
    void validateToken_shouldRejectTokenFromDifferentSecret() {
        JwtService otherService = new JwtService("different-secret", 3600000);
        String token = otherService.generateToken(createUser());
        assertNull(jwtService.validateToken(token));
    }

    @Test
    void validateToken_shouldRejectExpiredToken() {
        JwtService shortLivedService = new JwtService("test-secret-key-for-unit-tests", -1000);
        String token = shortLivedService.generateToken(createUser());
        assertNull(jwtService.validateToken(token));
    }
}
