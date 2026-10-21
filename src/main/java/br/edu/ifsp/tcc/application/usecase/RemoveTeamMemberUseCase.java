package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RemoveTeamMemberUseCase {

    private final TeamService teamService;

    public RemoveTeamMemberUseCase(TeamService teamService) {
        this.teamService = teamService;
    }

    /** Admins remove anyone; any member can leave the team by removing themselves. */
    @Transactional
    public void execute(Long teamId, Long userId, Long memberId) {
        TeamMember requester = teamService.requireMember(teamId, userId);
        TeamMember member = teamService.findMember(teamId, memberId);

        boolean leaving = member.getId().equals(requester.getId());
        if (!leaving && requester.getRole() != TeamRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe nao permite esta acao.");
        }
        if (member.getRole() == TeamRole.ADMIN && teamService.countAdmins(teamId) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A equipe precisa de pelo menos um administrador.");
        }
        teamService.deleteMember(member);
    }
}
