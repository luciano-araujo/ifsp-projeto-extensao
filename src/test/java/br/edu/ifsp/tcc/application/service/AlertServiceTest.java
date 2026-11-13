package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.dto.AlertType;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import br.edu.ifsp.tcc.application.service.AlertService.TeamAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @Mock private KanbanItemRepository kanbanItemRepository;
    @Mock private CardTaskRepository cardTaskRepository;

    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(kanbanItemRepository, cardTaskRepository, "America/Sao_Paulo");
    }

    private KanbanItem card(long id, String title, LocalDate due) {
        KanbanItem card = new KanbanItem();
        card.setId(id);
        card.setTitle(title);
        card.setDueDate(due);
        return card;
    }

    private CardTask task(long id, KanbanItem card, String title, LocalDate due, Long assigneeUserId) {
        CardTask task = new CardTask();
        task.setId(id);
        task.setKanbanItem(card);
        task.setTitle(title);
        task.setDueDate(due);
        if (assigneeUserId != null) {
            User user = new User();
            user.setId(assigneeUserId);
            TeamMember member = new TeamMember();
            member.setUser(user);
            task.setAssignee(member);
        }
        return task;
    }

    @Test
    void today_shouldBeTheDateInSaoPauloNotInUtc() {
        assertEquals(LocalDate.now(ZoneId.of("America/Sao_Paulo")), service.today());
    }

    @Test
    void findTeamAlerts_shouldClassifyByDaysLeftAndSortByClosestDeadline() {
        KanbanItem overdue = card(15, "Webinar", TODAY.minusDays(1));
        KanbanItem dueToday = card(16, "Hoje", TODAY);
        KanbanItem dueInThree = card(17, "Em três dias", TODAY.plusDays(3));
        KanbanItem taskCard = card(14, "Videoaula", null);
        when(kanbanItemRepository.findProjectsDueUntil(10L, TODAY.plusDays(3)))
                .thenReturn(List.of(dueInThree, dueToday, overdue));
        when(cardTaskRepository.findOpenDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(
                task(33, taskCard, "Revisar o roteiro", TODAY.plusDays(2), 5L),
                task(34, taskCard, "Gravar", TODAY.minusDays(3), null)));

        List<TeamAlert> alerts = service.findTeamAlerts(10L, TODAY);

        assertEquals(List.of(AlertType.TASK_OVERDUE, AlertType.CARD_OVERDUE, AlertType.CARD_DUE_SOON,
                        AlertType.TASK_DUE_SOON, AlertType.CARD_DUE_SOON),
                alerts.stream().map(a -> a.alert().getType()).toList());
        AlertResponse first = alerts.get(0).alert();
        assertEquals(14L, first.getCardId());
        assertEquals(34L, first.getTaskId());
        assertEquals("Gravar", first.getTaskTitle());
        assertEquals(-3, first.getDaysLeft());
        assertNull(alerts.get(0).assigneeUserId());
        assertEquals(5L, alerts.get(3).assigneeUserId());
        assertEquals(2, alerts.get(3).alert().getDaysLeft());
        AlertResponse cardAlert = alerts.get(1).alert();
        assertNull(cardAlert.getTaskId());
        assertNull(cardAlert.getTaskTitle());
        assertEquals(0, alerts.get(2).alert().getDaysLeft());
        assertEquals(3, alerts.get(4).alert().getDaysLeft());
    }

    @Test
    void findTeamAlerts_shouldBeEmptyWhenNothingIsDue() {
        when(kanbanItemRepository.findProjectsDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of());
        when(cardTaskRepository.findOpenDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of());

        assertTrue(service.findTeamAlerts(10L, TODAY).isEmpty());
    }

    @Test
    void findTeamAlerts_shouldOrderEqualDatesByCardThenCardAlertBeforeItsTasks() {
        KanbanItem a = card(1, "A", TODAY);
        KanbanItem b = card(2, "B", TODAY);
        when(kanbanItemRepository.findProjectsDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(b, a));
        when(cardTaskRepository.findOpenDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(
                task(9, a, "t9", TODAY, null), task(8, a, "t8", TODAY, null)));

        List<TeamAlert> alerts = service.findTeamAlerts(10L, TODAY);

        assertEquals(List.of("1/null", "1/8", "1/9", "2/null"), alerts.stream()
                .map(x -> x.alert().getCardId() + "/" + x.alert().getTaskId()).toList());
    }

    @Test
    void concerns_shouldFollowTheDigestRule() {
        TeamAlert unassigned = new TeamAlert(null, null);
        TeamAlert assigned = new TeamAlert(null, 5L);

        assertTrue(unassigned.concerns(1L));
        assertTrue(assigned.concerns(5L));
        assertFalse(assigned.concerns(6L));
    }

    @Test
    void describe_shouldWriteTheLineInPortuguese() {
        AlertResponse overdueCard = new AlertResponse(AlertType.CARD_OVERDUE, 1L, "Aula 1", null, null,
                TODAY.minusDays(2), -2);
        AlertResponse overdueOne = new AlertResponse(AlertType.CARD_OVERDUE, 1L, "Aula 1", null, null,
                TODAY.minusDays(1), -1);
        AlertResponse today = new AlertResponse(AlertType.TASK_DUE_SOON, 1L, "Aula 1", 7L, "Revisar", TODAY, 0);
        AlertResponse tomorrow = new AlertResponse(AlertType.CARD_DUE_SOON, 1L, "Aula 1", null, null,
                TODAY.plusDays(1), 1);
        AlertResponse later = new AlertResponse(AlertType.CARD_DUE_SOON, 1L, "Aula 1", null, null,
                TODAY.plusDays(3), 3);

        assertEquals("[Equipe] Projeto \"Aula 1\": atrasado há 2 dias (2026-11-11)", service.describe("Equipe", overdueCard));
        assertEquals("[Equipe] Projeto \"Aula 1\": atrasado há 1 dia (2026-11-12)", service.describe("Equipe", overdueOne));
        assertEquals("[Equipe] Tarefa \"Revisar\" (projeto \"Aula 1\"): vence hoje (2026-11-13)", service.describe("Equipe", today));
        assertEquals("[Equipe] Projeto \"Aula 1\": vence em 1 dia (2026-11-14)", service.describe("Equipe", tomorrow));
        assertEquals("[Equipe] Projeto \"Aula 1\": vence em 3 dias (2026-11-16)", service.describe("Equipe", later));
    }
}
