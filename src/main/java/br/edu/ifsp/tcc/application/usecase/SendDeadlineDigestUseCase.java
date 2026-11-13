package br.edu.ifsp.tcc.application.usecase;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Three queries in total, whatever the number of teams: alerts (projects, tasks) and members of the teams with alerts.
     */
    @Transactional(readOnly = true)
    public int execute() {
        List<AlertService.DigestAlert> alerts = alertService.findAllAlerts(alertService.today());
        if (alerts.isEmpty()) {
            return 0;
        }

        Set<Long> teamIds = new LinkedHashSet<>();
        alerts.forEach(a -> teamIds.add(a.teamId()));
        Map<Long, List<TeamMember>> membersByTeam = new LinkedHashMap<>();
        for (TeamMember member : teamService.findMembersOfTeams(teamIds)) {
            membersByTeam.computeIfAbsent(member.getTeam().getId(), k -> new ArrayList<>()).add(member);
        }

        Map<Long, User> users = new LinkedHashMap<>();
        Map<Long, List<String>> linesByUser = new LinkedHashMap<>();
        for (AlertService.DigestAlert digestAlert : alerts) {
            for (TeamMember member : membersByTeam.getOrDefault(digestAlert.teamId(), List.of())) {
                User user = member.getUser();
                if (digestAlert.alert().concerns(user.getId())) {
                    users.putIfAbsent(user.getId(), user);
                    linesByUser.computeIfAbsent(user.getId(), k -> new ArrayList<>())
                            .add(alertService.describe(digestAlert.teamName(), digestAlert.alert().alert()));
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
