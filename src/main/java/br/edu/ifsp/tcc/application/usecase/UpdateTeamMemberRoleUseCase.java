package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamMemberResponse;
import br.edu.ifsp.tcc.application.dto.UpdateTeamMemberDTO;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UpdateTeamMemberRoleUseCase {

    private final TeamService teamService;

    public UpdateTeamMemberRoleUseCase(TeamService teamService) {
        this.teamService = teamService;
    }

    @Transactional
    public TeamMemberResponse execute(Long teamId, Long userId, Long memberId, UpdateTeamMemberDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        TeamMember member = teamService.findMember(teamId, memberId);

        // The team must always keep at least one administrator
        if (member.getRole() == TeamRole.ADMIN && dto.getRole() != TeamRole.ADMIN && teamService.countAdmins(teamId) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A equipe precisa de pelo menos um administrador.");
        }
        member.setRole(dto.getRole());
        return teamService.toMemberResponse(teamService.saveMember(member));
    }
}
