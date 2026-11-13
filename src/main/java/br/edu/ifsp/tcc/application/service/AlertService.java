package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.dto.AlertType;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AlertService {

    /** Product decision 6: "due soon" means up to 3 days. */
    public static final int DUE_SOON_DAYS = 3;

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** An alert plus who it is about: the assignee of a task, or null (card alerts and unassigned tasks). */
    public record TeamAlert(AlertResponse alert, Long assigneeUserId) {
        /** Digest rule: card alerts and unassigned task alerts go to every member; assigned tasks to the assignee only. */
        public boolean concerns(Long userId) {
            return assigneeUserId == null || assigneeUserId.equals(userId);
        }
    }

    /** An alert of any team, with the team it belongs to (daily digest). */
    public record DigestAlert(Long teamId, String teamName, TeamAlert alert) { }

    private final KanbanItemRepository kanbanItemRepository;
    private final CardTaskRepository cardTaskRepository;
    private final CardReviewService cardReviewService;
    private final ZoneId zone;

    public AlertService(KanbanItemRepository kanbanItemRepository,
                        CardTaskRepository cardTaskRepository,
                        CardReviewService cardReviewService,
                        @Value("${app.alerts.timezone:America/Sao_Paulo}") String zone) {
        this.kanbanItemRepository = kanbanItemRepository;
        this.cardTaskRepository = cardTaskRepository;
        this.cardReviewService = cardReviewService;
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
        List<KanbanItem> cards = kanbanItemRepository.findProjectsDueUntil(teamId, limit);
        List<CardTask> tasks = cardTaskRepository.findOpenDueUntil(teamId, limit);
        Set<Long> approved = approvedCardIds(cards, tasks);
        cards.stream().filter(c -> !approved.contains(c.getId())).forEach(card -> alerts.add(cardAlert(card, today)));
        tasks.stream().filter(t -> !approved.contains(t.getKanbanItem().getId())).forEach(task -> alerts.add(taskAlert(task, today)));
        alerts.sort(ALERT_ORDER);
        return alerts;
    }

    /**
     * The alerts of every team with two queries (one for projects, one for tasks), for the daily digest.
     * Ordered by team and then by closest deadline.
     */
    public List<DigestAlert> findAllAlerts(LocalDate today) {
        LocalDate limit = today.plusDays(DUE_SOON_DAYS);
        List<DigestAlert> alerts = new ArrayList<>();
        List<KanbanItem> cards = kanbanItemRepository.findAllProjectsDueUntil(limit);
        List<CardTask> tasks = cardTaskRepository.findAllOpenDueUntil(limit);
        Set<Long> approved = approvedCardIds(cards, tasks);
        for (KanbanItem card : cards) {
            if (approved.contains(card.getId())) continue;
            alerts.add(new DigestAlert(card.getTeamId(), card.getTeam().getName(), cardAlert(card, today)));
        }
        for (CardTask task : tasks) {
            if (approved.contains(task.getKanbanItem().getId())) continue;
            KanbanItem card = task.getKanbanItem();
            alerts.add(new DigestAlert(card.getTeamId(), card.getTeam().getName(), taskAlert(task, today)));
        }
        alerts.sort(Comparator.comparing(DigestAlert::teamId)
                .thenComparing(DigestAlert::alert, ALERT_ORDER));
        return alerts;
    }

    /**
     * Decision 19: alerts cover active projects only, so projects whose latest review is APPROVED (and their tasks)
     * are left out. One query for the statuses of all the cards involved.
     */
    private Set<Long> approvedCardIds(List<KanbanItem> cards, List<CardTask> tasks) {
        Map<Long, KanbanItem> involved = new HashMap<>();
        cards.forEach(c -> involved.put(c.getId(), c));
        tasks.forEach(t -> involved.putIfAbsent(t.getKanbanItem().getId(), t.getKanbanItem()));
        Set<Long> approved = new java.util.HashSet<>();
        cardReviewService.findStatuses(List.copyOf(involved.values()))
                .forEach((id, status) -> { if (status == ReviewStatus.APPROVED) approved.add(id); });
        return approved;
    }

    private static final Comparator<TeamAlert> ALERT_ORDER = Comparator
            .comparing((TeamAlert a) -> a.alert().getDueDate())
            .thenComparing(a -> a.alert().getCardId())
            .thenComparing(a -> a.alert().getTaskId() == null ? 0L : a.alert().getTaskId());

    private TeamAlert cardAlert(KanbanItem card, LocalDate today) {
        int daysLeft = daysLeft(today, card.getDueDate());
        return new TeamAlert(new AlertResponse(
                daysLeft < 0 ? AlertType.CARD_OVERDUE : AlertType.CARD_DUE_SOON,
                card.getId(), card.getTitle(), null, null, card.getDueDate(), daysLeft), null);
    }

    private TeamAlert taskAlert(CardTask task, LocalDate today) {
        int daysLeft = daysLeft(today, task.getDueDate());
        KanbanItem card = task.getKanbanItem();
        TeamMember assignee = task.getAssignee();
        return new TeamAlert(new AlertResponse(
                daysLeft < 0 ? AlertType.TASK_OVERDUE : AlertType.TASK_DUE_SOON,
                card.getId(), card.getTitle(), task.getId(), task.getTitle(), task.getDueDate(), daysLeft),
                assignee == null ? null : assignee.getUser().getId());
    }

    /** One line of the digest e-mail. */
    public String describe(String teamName, AlertResponse alert) {
        String subject = alert.getTaskTitle() == null
                ? "Projeto \"" + clean(alert.getCardTitle()) + "\""
                : "Tarefa \"" + clean(alert.getTaskTitle()) + "\" (projeto \"" + clean(alert.getCardTitle()) + "\")";
        int days = alert.getDaysLeft();
        String when;
        if (days < 0) {
            when = "venceu há " + (-days) + (days == -1 ? " dia" : " dias");
        } else if (days == 0) {
            when = "vence hoje";
        } else if (days == 1) {
            when = "vence amanhã";
        } else {
            when = "vence em " + days + " dias";
        }
        return "[" + clean(teamName) + "] " + subject + ": " + when + " (" + alert.getDueDate().format(BR_DATE) + ")";
    }

    /** Defence in depth for data created before titles refused control characters: a line break must not forge a line. */
    private static String clean(String text) {
        return text == null ? "" : text.replaceAll("[\r\n\t]", " ");
    }

    private static int daysLeft(LocalDate today, LocalDate dueDate) {
        return (int) ChronoUnit.DAYS.between(today, dueDate);
    }
}
