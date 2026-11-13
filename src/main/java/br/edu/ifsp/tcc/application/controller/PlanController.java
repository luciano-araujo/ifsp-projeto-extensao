package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.PlanResponse;
import br.edu.ifsp.tcc.application.dto.StartTrialDTO;
import br.edu.ifsp.tcc.application.dto.TeamPlanResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.GetTeamPlanUseCase;
import br.edu.ifsp.tcc.application.usecase.ListPlansUseCase;
import br.edu.ifsp.tcc.application.usecase.StartTrialUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PlanController {

    private final ListPlansUseCase listPlansUseCase;
    private final GetTeamPlanUseCase getTeamPlanUseCase;
    private final StartTrialUseCase startTrialUseCase;

    public PlanController(ListPlansUseCase listPlansUseCase,
                          GetTeamPlanUseCase getTeamPlanUseCase,
                          StartTrialUseCase startTrialUseCase) {
        this.listPlansUseCase = listPlansUseCase;
        this.getTeamPlanUseCase = getTeamPlanUseCase;
        this.startTrialUseCase = startTrialUseCase;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> list() {
        return ResponseEntity.ok(listPlansUseCase.execute());
    }

    @GetMapping("/teams/{teamId}/plan")
    public ResponseEntity<TeamPlanResponse> get(@AuthenticationPrincipal User user, @PathVariable Long teamId) {
        return ResponseEntity.ok(getTeamPlanUseCase.execute(teamId, user.getId()));
    }

    @PostMapping("/teams/{teamId}/plan/trial")
    public ResponseEntity<TeamPlanResponse> startTrial(@AuthenticationPrincipal User user,
                                                       @PathVariable Long teamId,
                                                       @Valid @RequestBody StartTrialDTO dto) {
        return ResponseEntity.ok(startTrialUseCase.execute(teamId, user.getId(), dto));
    }
}
