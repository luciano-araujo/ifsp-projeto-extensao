package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteInviteUseCase {

    private final TeamService teamService;
    private final InviteService inviteService;

    public DeleteInviteUseCase(TeamService teamService, InviteService inviteService) {
        this.teamService = teamService;
        this.inviteService = inviteService;
    }

    /** Cancels an invite of the team (ADMIN); an invite of another team is a 404. */
    @Transactional
    public void execute(Long teamId, Long userId, Long inviteId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        inviteService.delete(inviteService.findOfTeam(teamId, inviteId));
    }
}
