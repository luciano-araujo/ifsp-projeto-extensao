package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    @Test
    void isExpired_shouldBeTrueAfterExpiration() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertTrue(token.isExpired());
    }

    @Test
    void isExpired_shouldBeFalseBeforeExpiration() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        assertFalse(token.isExpired());
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        PasswordResetToken token = new PasswordResetToken();

        token.onCreate();

        assertNotNull(token.getCreatedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(1L);
        token.setEmail("ana@teste.com");
        token.setToken("123456");
        token.setAttempts(2);

        assertEquals(1L, token.getId());
        assertEquals("ana@teste.com", token.getEmail());
        assertEquals("123456", token.getToken());
        assertEquals(2, token.getAttempts());
    }
}
