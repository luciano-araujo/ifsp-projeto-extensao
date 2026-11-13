package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.dto.AlertType;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import br.edu.ifsp.tcc.application.service.AlertService.DigestAlert;
import br.edu.ifsp.tcc.application.service.AlertService.TeamAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @Mock private KanbanItemRepository kanbanItemRepository;
    @Mock private CardTaskRepository cardTaskRepository;
    @Mock private CardReviewService cardReviewService;

    private AlertService service;

    @BeforeEach
    void setUp() {
        service = new AlertService(kanbanItemRepository, cardTaskRepository, cardReviewService, "America/Sao_Paulo");
    }

    private KanbanItem card(long id, String title, LocalDate due) {
        KanbanItem card = new KanbanItem();
        card.setId(id);
        card.setTitle(title);
        card.setDueDate(due);
        Team team = new Team();
        team.setId(10L + id % 2);
        team.setName("Equipe " + (10L + id % 2));
        card.setTeam(team);
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
        when(cardReviewService.findStatuses(anyList())).thenReturn(Map.of());

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
        when(cardReviewService.findStatuses(anyList())).thenReturn(Map.of());

        assertTrue(service.findTeamAlerts(10L, TODAY).isEmpty());
    }

    @Test
    void findTeamAlerts_shouldOrderEqualDatesByCardThenCardAlertBeforeItsTasks() {
        KanbanItem a = card(1, "A", TODAY);
        KanbanItem b = card(2, "B", TODAY);
        when(kanbanItemRepository.findProjectsDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(b, a));
        when(cardTaskRepository.findOpenDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(
                task(9, a, "t9", TODAY, null), task(8, a, "t8", TODAY, null)));
        when(cardReviewService.findStatuses(anyList())).thenReturn(Map.of());

        List<TeamAlert> alerts = service.findTeamAlerts(10L, TODAY);

        assertEquals(List.of("1/null", "1/8", "1/9", "2/null"), alerts.stream()
                .map(x -> x.alert().getCardId() + "/" + x.alert().getTaskId()).toList());
    }

    @Test
    void findTeamAlerts_shouldLeaveOutApprovedProjectsAndTheirTasksButKeepOthers() {
        KanbanItem approved = card(1, "Aprovado", TODAY.minusDays(2));
        KanbanItem changes = card(2, "Com ajustes", TODAY.minusDays(2));
        KanbanItem unreviewed = card(3, "Sem revisão", TODAY.minusDays(2));
        when(kanbanItemRepository.findProjectsDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(approved, changes, unreviewed));
        when(cardTaskRepository.findOpenDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(
                task(1, approved, "do aprovado", TODAY.minusDays(1), null),
                task(2, changes, "do com ajustes", TODAY.minusDays(1), null)));
        when(cardReviewService.findStatuses(anyList())).thenReturn(Map.of(
                1L, ReviewStatus.APPROVED, 2L, ReviewStatus.CHANGES_REQUESTED));

        List<TeamAlert> alerts = service.findTeamAlerts(10L, TODAY);

        assertEquals(List.of("2/null", "3/null", "2/2"), alerts.stream()
                .map(x -> x.alert().getCardId() + "/" + x.alert().getTaskId()).toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findTeamAlerts_shouldAskForTheStatusesOfAllInvolvedCardsInOneCall() {
        KanbanItem a = card(1, "A", TODAY);
        KanbanItem b = card(2, "B", null);
        when(kanbanItemRepository.findProjectsDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(a));
        when(cardTaskRepository.findOpenDueUntil(10L, TODAY.plusDays(3))).thenReturn(List.of(
                task(1, a, "t", TODAY, null), task(2, b, "u", TODAY, null)));
        when(cardReviewService.findStatuses(anyList())).thenReturn(Map.of());

        service.findTeamAlerts(10L, TODAY);

        ArgumentCaptor<List<KanbanItem>> items = ArgumentCaptor.forClass(List.class);
        verify(cardReviewService, times(1)).findStatuses(items.capture());
        assertEquals(2, items.getValue().size());
    }

    @Test
    void findAllAlerts_shouldCoverEveryTeamWithTwoQueriesAndSkipApprovedProjects() {
        KanbanItem inTeam11 = card(1, "Do time 11", TODAY.minusDays(1));
        KanbanItem inTeam10 = card(2, "Do time 10", TODAY.plusDays(1));
        KanbanItem approved = card(3, "Aprovado", TODAY.minusDays(5));
        when(kanbanItemRepository.findAllProjectsDueUntil(TODAY.plusDays(3))).thenReturn(List.of(inTeam11, inTeam10, approved));
        when(cardTaskRepository.findAllOpenDueUntil(TODAY.plusDays(3))).thenReturn(List.of(
                task(7, inTeam10, "tarefa", TODAY, 4L), task(8, approved, "tarefa do aprovado", TODAY, null)));
        when(cardReviewService.findStatuses(anyList())).thenReturn(Map.of(3L, ReviewStatus.APPROVED));

        List<DigestAlert> alerts = service.findAllAlerts(TODAY);

        // by team (10 first), then closest deadline
        assertEquals(List.of("10/2/7", "10/2/null", "11/1/null"), alerts.stream()
                .map(a -> a.teamId() + "/" + a.alert().alert().getCardId() + "/" + a.alert().alert().getTaskId()).toList());
        assertEquals("Equipe 10", alerts.get(0).teamName());
        assertEquals(4L, alerts.get(0).alert().assigneeUserId());
        verify(kanbanItemRepository).findAllProjectsDueUntil(TODAY.plusDays(3));
        verify(cardTaskRepository).findAllOpenDueUntil(TODAY.plusDays(3));
        verifyNoMoreInteractions(kanbanItemRepository, cardTaskRepository);
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
    void describe_shouldWriteTheLineInPortugueseWithBrazilianDates() {
        AlertResponse overdue2 = new AlertResponse(AlertType.CARD_OVERDUE, 1L, "Aula 1", null, null, TODAY.minusDays(2), -2);
        AlertResponse overdue1 = new AlertResponse(AlertType.CARD_OVERDUE, 1L, "Aula 1", null, null, TODAY.minusDays(1), -1);
        AlertResponse today = new AlertResponse(AlertType.TASK_DUE_SOON, 1L, "Aula 1", 7L, "Revisar", TODAY, 0);
        AlertResponse tomorrow = new AlertResponse(AlertType.CARD_DUE_SOON, 1L, "Aula 1", null, null, TODAY.plusDays(1), 1);
        AlertResponse later = new AlertResponse(AlertType.CARD_DUE_SOON, 1L, "Aula 1", null, null, TODAY.plusDays(3), 3);

        assertEquals("[Equipe] Projeto \"Aula 1\": venceu há 2 dias (11/11/2026)", service.describe("Equipe", overdue2));
        assertEquals("[Equipe] Projeto \"Aula 1\": venceu há 1 dia (12/11/2026)", service.describe("Equipe", overdue1));
        assertEquals("[Equipe] Tarefa \"Revisar\" (projeto \"Aula 1\"): vence hoje (13/11/2026)", service.describe("Equipe", today));
        assertEquals("[Equipe] Projeto \"Aula 1\": vence amanhã (14/11/2026)", service.describe("Equipe", tomorrow));
        assertEquals("[Equipe] Projeto \"Aula 1\": vence em 3 dias (16/11/2026)", service.describe("Equipe", later));
    }

    @Test
    void describe_shouldNeverLetALineBreakInATitleForgeAnotherLine() {
        AlertResponse alert = new AlertResponse(AlertType.TASK_DUE_SOON, 1L, "Projeto\r\nAVISO: falso", 7L, "Tarefa\tcom\ntab",
                TODAY, 0);

        String line = service.describe("Equipe\nX", alert);

        assertFalse(line.contains("\n") || line.contains("\r") || line.contains("\t"), line);
        assertTrue(line.contains("Projeto  AVISO: falso"));
    }

    @Test
    void describe_shouldCopeWithMissingTitles() {
        AlertResponse alert = new AlertResponse(AlertType.CARD_DUE_SOON, 1L, null, null, null, TODAY, 0);

        assertEquals("[Equipe] Projeto \"\": vence hoje (13/11/2026)", service.describe("Equipe", alert));
    }
}
