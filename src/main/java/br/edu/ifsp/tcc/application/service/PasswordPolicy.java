package br.edu.ifsp.tcc.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

/** BCrypt only accepts passwords up to 72 bytes; longer ones must be a 400, not an error while encoding. */
public final class PasswordPolicy {

    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void requireEncodable(String password) {
        if (password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha deve ter no máximo " + MAX_BYTES + " bytes (caracteres acentuados contam mais).");
        }
    }
}
