package br.edu.ifsp.tcc.application.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecretValidatorTest {

    @Test
    void validate_shouldAcceptASecretOf32Bytes() {
        assertDoesNotThrow(() -> new JwtSecretValidator("a".repeat(32)).validate());
    }

    @Test
    void validate_shouldRefuseAShortSecretWithAClearMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtSecretValidator("a".repeat(31)).validate());
        assertTrue(ex.getMessage().contains("JWT_SECRET"));
        assertTrue(ex.getMessage().contains("32"));
    }

    @Test
    void validate_shouldCountBytesNotCharacters() {
        // 16 accented characters = 32 bytes: accepted; 15 = 30 bytes: refused
        assertDoesNotThrow(() -> new JwtSecretValidator("é".repeat(16)).validate());
        assertThrows(IllegalStateException.class, () -> new JwtSecretValidator("é".repeat(15)).validate());
    }

    @Test
    void validate_shouldRefuseAMissingSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtSecretValidator(null).validate());
    }
}
