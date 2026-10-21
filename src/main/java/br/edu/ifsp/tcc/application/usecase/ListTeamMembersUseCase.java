package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamMemberResponse;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListTeamMembersUseCase {

    private final TeamService teamService;

    public ListTeamMembersUseCase(TeamService teamService) {
        this.teamService = teamService;
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> execute(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return teamService.findMembersByTeamId(teamId).stream()
                .map(teamService::toMemberResponse)
                .toList();
    }
}
