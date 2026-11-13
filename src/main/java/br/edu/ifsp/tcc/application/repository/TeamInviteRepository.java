package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.InviteStatus;
import br.edu.ifsp.tcc.application.entity.TeamInvite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {

    @EntityGraph(attributePaths = {"team", "invitedBy"})
    Optional<TeamInvite> findByTokenHash(String tokenHash);

    @EntityGraph(attributePaths = "invitedBy")
    List<TeamInvite> findByTeam_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
            Long teamId, InviteStatus status, LocalDateTime now);

    Optional<TeamInvite> findByIdAndTeam_Id(Long id, Long teamId);

    List<TeamInvite> findByTeam_IdAndEmailIgnoreCaseAndStatus(Long teamId, String email, InviteStatus status);

    @Modifying
    @Query("DELETE FROM TeamInvite i WHERE i.status = br.edu.ifsp.tcc.application.entity.InviteStatus.PENDING "
            + "AND i.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
