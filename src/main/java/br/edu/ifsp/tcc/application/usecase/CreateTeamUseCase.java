package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateTeamDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateTeamUseCase {

    private final TeamService teamService;
    private final PlanService planService;

    public CreateTeamUseCase(TeamService teamService, PlanService planService) {
        this.teamService = teamService;
        this.planService = planService;
    }

    @Transactional
    public TeamResponse execute(User user, CreateTeamDTO dto) {
        planService.requireCanCreateFreeTeam(user.getId());
        return teamService.toTeamResponse(teamService.createTeamWithAdmin(user, dto.getName().trim()));
    }
}
