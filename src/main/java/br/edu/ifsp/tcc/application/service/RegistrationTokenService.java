package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.repository.RegistrationTokenRepository;
import org.springframework.stereotype.Service;

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

    public Optional<RegistrationToken> findByEmailAndToken(String email, String token) {
        return registrationTokenRepository.findByEmailAndToken(email, token);
    }

    public void deleteByEmail(String email) {
        registrationTokenRepository.deleteByEmail(email);
    }

    public void delete(RegistrationToken token) {
        registrationTokenRepository.delete(token);
    }
}
