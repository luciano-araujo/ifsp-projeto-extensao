package br.edu.ifsp.tcc.application.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Production only: refuses to start with a JWT secret shorter than 32 bytes (local and it profiles are not checked). */
@Component
@Profile("prod")
public class JwtSecretValidator {

    static final int MIN_BYTES = 32;

    private final String secret;

    public JwtSecretValidator(@Value("${app.jwt.secret}") String secret) {
        this.secret = secret;
    }

    @PostConstruct
    void validate() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET (app.jwt.secret) deve ter pelo menos " + MIN_BYTES + " bytes no perfil prod.");
        }
    }
}
