package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.RenameTeamDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RenameTeamUseCase {

    private final TeamService teamService;

    public RenameTeamUseCase(TeamService teamService) {
        this.teamService = teamService;
    }

    @Transactional
    public TeamResponse execute(Long teamId, Long userId, RenameTeamDTO dto) {
        TeamMember admin = teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        teamService.renameTeam(admin.getTeam(), dto.getName().trim());
        return teamService.toTeamResponse(admin);
    }
}
