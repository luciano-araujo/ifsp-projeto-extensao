package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.EmailChangeToken;
import br.edu.ifsp.tcc.application.repository.EmailChangeTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmailChangeTokenService {

    private final EmailChangeTokenRepository emailChangeTokenRepository;

    public EmailChangeTokenService(EmailChangeTokenRepository emailChangeTokenRepository) {
        this.emailChangeTokenRepository = emailChangeTokenRepository;
    }

    public EmailChangeToken save(EmailChangeToken token) {
        return emailChangeTokenRepository.save(token);
    }

    public Optional<EmailChangeToken> findByUserId(Long userId) {
        return emailChangeTokenRepository.findByUser_Id(userId);
    }

    public void deleteByUserId(Long userId) {
        emailChangeTokenRepository.deleteByUserId(userId);
    }

    /** Removes every code that expired before the given moment; returns how many were removed. */
    public int deleteExpired(LocalDateTime now) {
        return emailChangeTokenRepository.deleteExpired(now);
    }

    /** Counts one wrong attempt atomically; false when the limit was already reached (the code must be discarded). */
    public boolean registerWrongAttempt(Long tokenId, int limit) {
        return emailChangeTokenRepository.incrementAttempts(tokenId, limit) > 0;
    }
}
