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
}
