package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ActivatePlanDTO;
import br.edu.ifsp.tcc.application.dto.TeamPlanResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.SystemAdminService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ActivateTeamPlanUseCase {

    private final SystemAdminService systemAdminService;
    private final PlanService planService;

    public ActivateTeamPlanUseCase(SystemAdminService systemAdminService, PlanService planService) {
        this.systemAdminService = systemAdminService;
        this.planService = planService;
    }

    /** System administrators only: sets the plan of any team (payment is handled outside the system). */
    @Transactional
    public TeamPlanResponse execute(User user, Long teamId, ActivatePlanDTO dto) {
        systemAdminService.requireSystemAdmin(user);
        return planService.toPlanResponse(
                planService.activate(planService.findTeam(teamId), dto.getPlan(), dto.getExpiresAt()));
    }
}
