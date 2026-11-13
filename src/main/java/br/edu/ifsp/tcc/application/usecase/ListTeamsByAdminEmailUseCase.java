package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AdminTeamResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.SystemAdminService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListTeamsByAdminEmailUseCase {

    private final SystemAdminService systemAdminService;
    private final UserService userService;
    private final PlanService planService;

    public ListTeamsByAdminEmailUseCase(SystemAdminService systemAdminService, UserService userService,
                                        PlanService planService) {
        this.systemAdminService = systemAdminService;
        this.userService = userService;
        this.planService = planService;
    }

    /** System administrators only: the teams administered by the e-mail (empty when nobody has it). */
    @Transactional(readOnly = true)
    public List<AdminTeamResponse> execute(User user, String email) {
        systemAdminService.requireSystemAdmin(user);
        return userService.findByEmail(email.trim())
                .map(found -> planService.findTeamsAdministeredBy(found.getId()).stream()
                        .map(planService::toAdminTeamResponse).toList())
                .orElse(List.of());
    }
}
