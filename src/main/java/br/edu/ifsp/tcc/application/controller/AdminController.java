package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.ActivatePlanDTO;
import br.edu.ifsp.tcc.application.dto.AdminTeamResponse;
import br.edu.ifsp.tcc.application.dto.TeamPlanResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.ActivateTeamPlanUseCase;
import br.edu.ifsp.tcc.application.usecase.ListTeamsByAdminEmailUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Routes for system administrators (e-mails in app.admin.emails). */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ActivateTeamPlanUseCase activateTeamPlanUseCase;
    private final ListTeamsByAdminEmailUseCase listTeamsByAdminEmailUseCase;

    public AdminController(ActivateTeamPlanUseCase activateTeamPlanUseCase,
                           ListTeamsByAdminEmailUseCase listTeamsByAdminEmailUseCase) {
        this.activateTeamPlanUseCase = activateTeamPlanUseCase;
        this.listTeamsByAdminEmailUseCase = listTeamsByAdminEmailUseCase;
    }

    @GetMapping("/teams")
    public ResponseEntity<List<AdminTeamResponse>> listTeams(@AuthenticationPrincipal User user,
                                                             @RequestParam String email) {
        return ResponseEntity.ok(listTeamsByAdminEmailUseCase.execute(user, email));
    }

    @PutMapping("/teams/{teamId}/plan")
    public ResponseEntity<TeamPlanResponse> activatePlan(@AuthenticationPrincipal User user,
                                                         @PathVariable Long teamId,
                                                         @Valid @RequestBody ActivatePlanDTO dto) {
        return ResponseEntity.ok(activateTeamPlanUseCase.execute(user, teamId, dto));
    }
}
