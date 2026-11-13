package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamPlanResponse;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetTeamPlanUseCase {

    private final TeamService teamService;
    private final PlanService planService;

    public GetTeamPlanUseCase(TeamService teamService, PlanService planService) {
        this.teamService = teamService;
        this.planService = planService;
    }

    /** Any member sees the plan in force, its limits and the usage. */
    @Transactional(readOnly = true)
    public TeamPlanResponse execute(Long teamId, Long userId) {
        return planService.toTeamPlanResponse(teamService.requireMember(teamId, userId).getTeam());
    }
}
