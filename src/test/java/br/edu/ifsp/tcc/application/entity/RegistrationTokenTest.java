package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationTokenTest {

    @Test
    void isExpired_shouldReturnTrueWhenPastExpiryTime() {
        RegistrationToken token = new RegistrationToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertTrue(token.isExpired());
    }

    @Test
    void isExpired_shouldReturnFalseWhenBeforeExpiryTime() {
        RegistrationToken token = new RegistrationToken();
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        assertFalse(token.isExpired());
    }

    @Test
    void gettersAndSetters_shouldWorkCorrectly() {
        RegistrationToken token = new RegistrationToken();
        token.setId(1L);
        token.setEmail("test@example.com");
        token.setName("Test");
        token.setToken("123456");
        token.setExpiresAt(LocalDateTime.of(2026, 12, 31, 23, 59));

        assertEquals(1L, token.getId());
        assertEquals("test@example.com", token.getEmail());
        assertEquals("Test", token.getName());
        assertEquals("123456", token.getToken());
        assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59), token.getExpiresAt());
    }
}
