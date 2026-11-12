package br.edu.ifsp.tcc.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    @Test
    void requireEncodable_shouldAcceptNullAndExactly72Bytes() {
        assertDoesNotThrow(() -> PasswordPolicy.requireEncodable(null));
        assertDoesNotThrow(() -> PasswordPolicy.requireEncodable("a".repeat(72)));
        assertDoesNotThrow(() -> PasswordPolicy.requireEncodable("é".repeat(36)));
    }

    @Test
    void requireEncodable_shouldCountBytesNotCharacters() {
        assertThrows(ResponseStatusException.class, () -> PasswordPolicy.requireEncodable("a".repeat(73)));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> PasswordPolicy.requireEncodable("é".repeat(37)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
