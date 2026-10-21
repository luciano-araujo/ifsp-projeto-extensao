package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class DeleteUserUseCase {

    private final UserService userService;
    private final TeamService teamService;
    private final KanbanItemService kanbanItemService;

    public DeleteUserUseCase(UserService userService, TeamService teamService, KanbanItemService kanbanItemService) {
        this.userService = userService;
        this.teamService = teamService;
        this.kanbanItemService = kanbanItemService;
    }

    /**
     * Deletes the account and the personal data (LGPD) without destroying the work of other people:
     * - the only administrator of a team with other members must promote someone first;
     * - teams where the user is the only member are deleted with all their content;
     * - in shared teams the cards stay with the team and only lose the author link (database SET NULL);
     * - cards that never joined a team are deleted.
     */
    @Transactional
    public void execute(Long id, Long authenticatedUserId) {
        if (!id.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }

        List<TeamMember> memberships = teamService.findMembershipsByUserId(id);
        for (TeamMember membership : memberships) {
            Long teamId = membership.getTeam().getId();
            boolean sharedTeam = teamService.countMembers(teamId) > 1;
            if (sharedTeam && membership.getRole() == TeamRole.ADMIN && teamService.countAdmins(teamId) <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Voce e o unico administrador da equipe \"" + membership.getTeam().getName()
                                + "\". Promova outro membro a administrador antes de excluir a conta.");
            }
        }

        for (TeamMember membership : memberships) {
            Team team = membership.getTeam();
            if (teamService.countMembers(team.getId()) == 1) {
                teamService.deleteTeam(team);
            }
        }

        for (KanbanItem item : kanbanItemService.findWithoutTeamByUserId(id)) {
            kanbanItemService.deleteById(item.getId());
        }

        userService.deleteById(id);
    }
}
