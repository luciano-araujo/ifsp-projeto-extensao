package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.StartTrialDTO;
import br.edu.ifsp.tcc.application.dto.TeamPlanResponse;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StartTrialUseCase {

    private final TeamService teamService;
    private final PlanService planService;

    public StartTrialUseCase(TeamService teamService, PlanService planService) {
        this.teamService = teamService;
        this.planService = planService;
    }

    /** The team administrator starts the 14-day trial of Solo, Pro or Agência, once per team. */
    // READ_COMMITTED + the team lock first: the plan check (count then act) must see what concurrent requests committed
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeamPlanResponse execute(Long teamId, Long userId, StartTrialDTO dto) {
        teamService.lockTeam(teamId);
        Team team = teamService.requireRole(teamId, userId, TeamRole.ADMIN).getTeam();
        return planService.toPlanResponse(planService.startTrial(team, dto.getPlan()));
    }
}
