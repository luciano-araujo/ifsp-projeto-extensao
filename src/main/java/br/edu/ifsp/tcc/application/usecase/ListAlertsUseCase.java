package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.service.AlertService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListAlertsUseCase {

    private final TeamService teamService;
    private final AlertService alertService;

    public ListAlertsUseCase(TeamService teamService, AlertService alertService) {
        this.teamService = teamService;
        this.alertService = alertService;
    }

    /** Overdue and due-soon (3 days) projects and tasks of the team, closest deadline first. Any member. */
    @Transactional(readOnly = true)
    public List<AlertResponse> execute(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return alertService.findTeamAlerts(teamId, alertService.today()).stream()
                .map(AlertService.TeamAlert::alert)
                .toList();
    }
}
