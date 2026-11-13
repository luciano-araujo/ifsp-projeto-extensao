package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListTeamsUseCase {

    private final TeamService teamService;

    public ListTeamsUseCase(TeamService teamService) {
        this.teamService = teamService;
    }

    /** Lists the teams of the user. A user without teams gets a personal team. */
    @Transactional
    public List<TeamResponse> execute(User user) {
        List<TeamMember> memberships = teamService.findMembershipsByUserId(user.getId());
        if (memberships.isEmpty()) {
            teamService.createPersonalTeam(user);
            memberships = teamService.findMembershipsByUserId(user.getId());
        }
        return memberships.stream().map(teamService::toTeamResponse).toList();
    }
}
