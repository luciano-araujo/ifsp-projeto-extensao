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
    // Bulk delete: two requests discarding the same code at once must not fail on a row that is already gone
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RegistrationToken t WHERE t.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM RegistrationToken t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    /**
     * Counts one wrong attempt atomically (the row is locked by the UPDATE, so parallel guesses cannot all read the same
     * counter). Returns 0 when the counter already reached the limit: the caller must discard the code.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RegistrationToken t SET t.attempts = t.attempts + 1 WHERE t.id = :id AND t.attempts < :limit")
    int incrementAttempts(@Param("id") Long id, @Param("limit") int limit);
}
