package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RegistrationTokenRepository extends JpaRepository<RegistrationToken, Long> {
    Optional<RegistrationToken> findFirstByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);

    @Modifying
    @Query("DELETE FROM RegistrationToken t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
