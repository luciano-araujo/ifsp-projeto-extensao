package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.PasswordResetToken;
import br.edu.ifsp.tcc.application.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public PasswordResetToken save(PasswordResetToken token) {
        return passwordResetTokenRepository.save(token);
    }

    public Optional<PasswordResetToken> findLatestByEmail(String email) {
        return passwordResetTokenRepository.findFirstByEmailOrderByCreatedAtDesc(email);
    }

    public void deleteByEmail(String email) {
        passwordResetTokenRepository.deleteByEmail(email);
    }

    /** Removes every code that expired before the given moment; returns how many were removed. */
    public int deleteExpired(LocalDateTime now) {
        return passwordResetTokenRepository.deleteExpired(now);
    }
}
