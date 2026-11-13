package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.EmailChangeToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {
    Optional<EmailChangeToken> findByUser_Id(Long userId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM EmailChangeToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailChangeToken t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    /**
     * Counts one wrong attempt atomically (the row is locked by the UPDATE, so parallel guesses cannot all read the same
     * counter). Returns 0 when the counter already reached the limit: the caller must discard the code.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE EmailChangeToken t SET t.attempts = t.attempts + 1 WHERE t.id = :id AND t.attempts < :limit")
    int incrementAttempts(@Param("id") Long id, @Param("limit") int limit);

    /** The counter as stored (read inside the transaction that just incremented it, so it includes that increment). */
    @Query("SELECT t.attempts FROM EmailChangeToken t WHERE t.id = :id")
    Integer findAttempts(@Param("id") Long id);
}
