package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUser_IdOrderByJoinedAtAsc(Long userId);
    List<TeamMember> findByTeam_IdOrderByJoinedAtAsc(Long teamId);
    Optional<TeamMember> findByTeam_IdAndUser_Id(Long teamId, Long userId);
    Optional<TeamMember> findByIdAndTeam_Id(Long id, Long teamId);
    int countByTeam_Id(Long teamId);

    // Members of several teams with their users in one query (daily digest)
    @Query("SELECT m FROM TeamMember m JOIN FETCH m.user WHERE m.team.id IN :teamIds ORDER BY m.team.id, m.joinedAt, m.id")
    List<TeamMember> findByTeamIdsWithUser(@Param("teamIds") Collection<Long> teamIds);
    int countByTeam_IdAndRole(Long teamId, TeamRole role);
}
