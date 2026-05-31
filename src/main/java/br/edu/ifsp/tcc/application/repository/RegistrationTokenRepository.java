package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
    Optional<RegistrationToken> findByEmailAndToken(String email, String token);
    void deleteByEmail(String email);
}
