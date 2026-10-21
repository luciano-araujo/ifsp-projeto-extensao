package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findByUser_IdOrderByJoinedAtAsc(Long userId);
    List<TeamMember> findByTeam_IdOrderByJoinedAtAsc(Long teamId);
    Optional<TeamMember> findByTeam_IdAndUser_Id(Long teamId, Long userId);
    Optional<TeamMember> findByIdAndTeam_Id(Long id, Long teamId);
    int countByTeam_Id(Long teamId);
    int countByTeam_IdAndRole(Long teamId, TeamRole role);
}
