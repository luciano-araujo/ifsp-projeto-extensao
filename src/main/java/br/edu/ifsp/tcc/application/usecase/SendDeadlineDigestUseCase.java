package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.AlertService;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SendDeadlineDigestUseCase {

    private final TeamService teamService;
    private final AlertService alertService;
    private final EmailService emailService;

    public SendDeadlineDigestUseCase(TeamService teamService, AlertService alertService, EmailService emailService) {
        this.teamService = teamService;
        this.alertService = alertService;
        this.emailService = emailService;
    }

    /**
     * RF09: one e-mail per user with the alerts of all their teams; users without alerts get nothing.
     * Rule: every member receives the card alerts of their teams; a task alert goes to its assignee only,
     * or to every member when the task has no assignee. Returns how many e-mails were sent.
     */
    @Transactional(readOnly = true)
    public int execute() {
        LocalDate today = alertService.today();
        Map<Long, User> users = new LinkedHashMap<>();
        Map<Long, List<String>> linesByUser = new LinkedHashMap<>();

        for (Team team : teamService.findAllTeams()) {
            List<AlertService.TeamAlert> alerts = alertService.findTeamAlerts(team.getId(), today);
            if (alerts.isEmpty()) {
                continue;
            }
            for (TeamMember member : teamService.findMembersByTeamId(team.getId())) {
                User user = member.getUser();
                for (AlertService.TeamAlert alert : alerts) {
                    if (alert.concerns(user.getId())) {
                        users.putIfAbsent(user.getId(), user);
                        linesByUser.computeIfAbsent(user.getId(), k -> new ArrayList<>())
                                .add(alertService.describe(team.getName(), alert.alert()));
                    }
                }
            }
        }

        linesByUser.forEach((userId, lines) -> {
            User user = users.get(userId);
            emailService.sendDeadlineDigest(user.getEmail(), user.getName(), lines);
        });
        return linesByUser.size();
    }
}
