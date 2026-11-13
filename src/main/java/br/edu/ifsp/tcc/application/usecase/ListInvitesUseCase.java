package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListInvitesUseCase {

    private final TeamService teamService;
    private final InviteService inviteService;

    public ListInvitesUseCase(TeamService teamService, InviteService inviteService) {
        this.teamService = teamService;
        this.inviteService = inviteService;
    }

    /** Pending (not accepted, not expired) invites of the team, newest first. ADMIN only. */
    @Transactional(readOnly = true)
    public List<InviteResponse> execute(Long teamId, Long userId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        return inviteService.findOpenByTeamId(teamId).stream().map(inviteService::toInviteResponse).toList();
    }
}
