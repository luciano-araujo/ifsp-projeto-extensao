package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.ListAlertsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/alerts")
public class AlertController {

    private final ListAlertsUseCase listAlertsUseCase;

    public AlertController(ListAlertsUseCase listAlertsUseCase) {
        this.listAlertsUseCase = listAlertsUseCase;
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> list(@AuthenticationPrincipal User user,
                                                    @PathVariable Long teamId) {
        return ResponseEntity.ok(listAlertsUseCase.execute(teamId, user.getId()));
    }
}
