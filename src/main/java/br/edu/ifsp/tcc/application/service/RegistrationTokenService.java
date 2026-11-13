package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.repository.RegistrationTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RegistrationTokenService {

    private final RegistrationTokenRepository registrationTokenRepository;

    public RegistrationTokenService(RegistrationTokenRepository registrationTokenRepository) {
        this.registrationTokenRepository = registrationTokenRepository;
    }

    public RegistrationToken save(RegistrationToken token) {
        return registrationTokenRepository.save(token);
    }

    public Optional<RegistrationToken> findLatestByEmail(String email) {
        return registrationTokenRepository.findFirstByEmailOrderByCreatedAtDesc(email);
    }

    public void deleteByEmail(String email) {
        registrationTokenRepository.deleteByEmail(email);
    }

    public void delete(RegistrationToken token) {
        registrationTokenRepository.delete(token);
    }

    /** Removes every code that expired before the given moment; returns how many were removed. */
    public int deleteExpired(LocalDateTime now) {
        return registrationTokenRepository.deleteExpired(now);
    }

    /**
     * Counts one attempt (right or wrong) atomically BEFORE the code is compared, so parallel guesses cannot go past the cap.
     * Returns the new counter, or -1 when the cap was already reached (the code must be discarded).
     */
    public int registerAttempt(Long tokenId, int cap) {
        if (registrationTokenRepository.incrementAttempts(tokenId, cap) == 0) return -1;
        return registrationTokenRepository.findAttempts(tokenId);
    }
}
