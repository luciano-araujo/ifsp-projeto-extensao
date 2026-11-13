package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.TeamInvite;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AcceptInviteUseCase {

    private final TeamService teamService;
    private final InviteService inviteService;

    public AcceptInviteUseCase(TeamService teamService, InviteService inviteService) {
        this.teamService = teamService;
        this.inviteService = inviteService;
    }

    /** The logged-in user joins the team with the role of the invite; only the invited e-mail may accept. */
    @Transactional
    public TeamResponse execute(String token, User user) {
        TeamInvite invite = inviteService.findOpenByToken(token);
        if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este convite foi enviado para outro e-mail.");
        }
        Long teamId = invite.getTeam().getId();
        if (teamService.findMemberByTeamIdAndUserId(teamId, user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já faz parte desta equipe.");
        }

        TeamMember member = new TeamMember();
        member.setTeam(invite.getTeam());
        member.setUser(user);
        member.setRole(invite.getRole());
        member = teamService.saveMember(member);
        inviteService.markAccepted(invite);
        return teamService.toTeamResponse(member);
    }
}
