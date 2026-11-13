package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteTeamUseCase {

    private final TeamService teamService;

    public DeleteTeamUseCase(TeamService teamService) {
        this.teamService = teamService;
    }

    /** Deletes the team with all its content (ADMIN); the personal team is refused with 409. */
    @Transactional
    public void execute(Long teamId, Long userId) {
        TeamMember admin = teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        teamService.requireDeletable(admin.getTeam());
        teamService.deleteTeam(admin.getTeam());
    }
}
