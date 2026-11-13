package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.dto.AlertType;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AlertService {

    /** Product decision 6: "due soon" means up to 3 days. */
    public static final int DUE_SOON_DAYS = 3;

    /** An alert plus who it is about: the assignee of a task, or null (card alerts and unassigned tasks). */
    public record TeamAlert(AlertResponse alert, Long assigneeUserId) {
        /** Digest rule: card alerts and unassigned task alerts go to every member; assigned tasks to the assignee only. */
        public boolean concerns(Long userId) {
            return assigneeUserId == null || assigneeUserId.equals(userId);
        }
    }

    private final KanbanItemRepository kanbanItemRepository;
    private final CardTaskRepository cardTaskRepository;
    private final ZoneId zone;

    public AlertService(KanbanItemRepository kanbanItemRepository,
                        CardTaskRepository cardTaskRepository,
                        @Value("${app.alerts.timezone:America/Sao_Paulo}") String zone) {
        this.kanbanItemRepository = kanbanItemRepository;
        this.cardTaskRepository = cardTaskRepository;
        this.zone = ZoneId.of(zone);
    }

    /** The calendar date of the users (the server runs in UTC, a day ahead of Brazil in the evening). */
    public LocalDate today() {
        return LocalDate.now(zone);
    }

    /** Overdue and due-soon projects and open tasks of the team, closest deadline first. */
    public List<TeamAlert> findTeamAlerts(Long teamId, LocalDate today) {
        LocalDate limit = today.plusDays(DUE_SOON_DAYS);
        List<TeamAlert> alerts = new ArrayList<>();
        for (KanbanItem card : kanbanItemRepository.findProjectsDueUntil(teamId, limit)) {
            int daysLeft = daysLeft(today, card.getDueDate());
            alerts.add(new TeamAlert(new AlertResponse(
                    daysLeft < 0 ? AlertType.CARD_OVERDUE : AlertType.CARD_DUE_SOON,
                    card.getId(), card.getTitle(), null, null, card.getDueDate(), daysLeft), null));
        }
        for (CardTask task : cardTaskRepository.findOpenDueUntil(teamId, limit)) {
            int daysLeft = daysLeft(today, task.getDueDate());
            KanbanItem card = task.getKanbanItem();
            TeamMember assignee = task.getAssignee();
            alerts.add(new TeamAlert(new AlertResponse(
                    daysLeft < 0 ? AlertType.TASK_OVERDUE : AlertType.TASK_DUE_SOON,
                    card.getId(), card.getTitle(), task.getId(), task.getTitle(), task.getDueDate(), daysLeft),
                    assignee == null ? null : assignee.getUser().getId()));
        }
        alerts.sort(Comparator
                .comparing((TeamAlert a) -> a.alert().getDueDate())
                .thenComparing(a -> a.alert().getCardId())
                .thenComparing(a -> a.alert().getTaskId() == null ? 0L : a.alert().getTaskId()));
        return alerts;
    }

    /** One line of the digest e-mail. */
    public String describe(String teamName, AlertResponse alert) {
        String subject = alert.getTaskTitle() == null
                ? "Projeto \"" + alert.getCardTitle() + "\""
                : "Tarefa \"" + alert.getTaskTitle() + "\" (projeto \"" + alert.getCardTitle() + "\")";
        int days = alert.getDaysLeft();
        String when;
        if (days < 0) {
            when = "atrasado há " + (-days) + (days == -1 ? " dia" : " dias");
        } else if (days == 0) {
            when = "vence hoje";
        } else {
            when = "vence em " + days + (days == 1 ? " dia" : " dias");
        }
        return "[" + teamName + "] " + subject + ": " + when + " (" + alert.getDueDate() + ")";
    }

    private static int daysLeft(LocalDate today, LocalDate dueDate) {
        return (int) ChronoUnit.DAYS.between(today, dueDate);
    }
}
