package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.dto.AlertType;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.AlertService;
import br.edu.ifsp.tcc.application.service.AlertService.TeamAlert;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertUseCasesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @Mock private TeamService teamService;
    @Mock private AlertService alertService;
    @Mock private EmailService emailService;

    private User user(long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@exemplo.test");
        return user;
    }

    private TeamMember member(User user) {
        TeamMember member = new TeamMember();
        member.setUser(user);
        member.setRole(TeamRole.CREATOR);
        return member;
    }

    private Team team(long id, String name) {
        Team team = new Team();
        team.setId(id);
        team.setName(name);
        return team;
    }

    private AlertResponse cardAlert() {
        return new AlertResponse(AlertType.CARD_OVERDUE, 1L, "Aula 1", null, null, TODAY.minusDays(1), -1);
    }

    private AlertResponse taskAlert() {
        return new AlertResponse(AlertType.TASK_DUE_SOON, 1L, "Aula 1", 7L, "Revisar", TODAY.plusDays(2), 2);
    }

    @Test
    void list_shouldReturnTheAlertsOfTheTeamForAnyMember() {
        when(alertService.today()).thenReturn(TODAY);
        when(alertService.findTeamAlerts(10L, TODAY))
                .thenReturn(List.of(new TeamAlert(cardAlert(), null), new TeamAlert(taskAlert(), 3L)));

        List<AlertResponse> result = new ListAlertsUseCase(teamService, alertService).execute(10L, 1L);

        assertEquals(List.of(cardAlert(), taskAlert()), result);
        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void list_shouldPropagateForbiddenForNonMembers() {
        when(teamService.requireMember(10L, 9L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));
        ListAlertsUseCase useCase = new ListAlertsUseCase(teamService, alertService);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 9L));
        verifyNoInteractions(alertService);
    }

    private SendDeadlineDigestUseCase digest() {
        return new SendDeadlineDigestUseCase(teamService, alertService, emailService);
    }

    @Test
    void digest_shouldSendOneEmailPerUserCoveringAllTheirTeamsAndSkipUsersWithoutAlerts() {
        User ana = user(1, "Ana");
        User bia = user(2, "Bia");
        User caio = user(3, "Caio");
        Team t1 = team(10L, "Equipe A");
        Team t2 = team(20L, "Equipe B");
        Team t3 = team(30L, "Sem prazos");
        when(alertService.today()).thenReturn(TODAY);
        when(teamService.findAllTeams()).thenReturn(List.of(t1, t2, t3));
        // team A: a card alert (everyone) + a task assigned to Bia; team B: an unassigned task; team C: nothing
        when(alertService.findTeamAlerts(10L, TODAY)).thenReturn(List.of(
                new TeamAlert(cardAlert(), null), new TeamAlert(taskAlert(), 2L)));
        when(alertService.findTeamAlerts(20L, TODAY)).thenReturn(List.of(new TeamAlert(taskAlert(), null)));
        when(alertService.findTeamAlerts(30L, TODAY)).thenReturn(List.of());
        when(teamService.findMembersByTeamId(10L)).thenReturn(List.of(member(ana), member(bia)));
        when(teamService.findMembersByTeamId(20L)).thenReturn(List.of(member(ana), member(caio)));
        when(alertService.describe(anyString(), any(AlertResponse.class)))
                .thenAnswer(inv -> inv.getArgument(0) + "|" + ((AlertResponse) inv.getArgument(1)).getType());

        int sent = digest().execute();

        assertEquals(3, sent);
        // Ana: card alert of A, unassigned task of B (the task of A is Bia's)
        verify(emailService).sendDeadlineDigest("ana@exemplo.test", "Ana",
                List.of("Equipe A|CARD_OVERDUE", "Equipe B|TASK_DUE_SOON"));
        // Bia: card alert + her own task of A
        verify(emailService).sendDeadlineDigest("bia@exemplo.test", "Bia",
                List.of("Equipe A|CARD_OVERDUE", "Equipe A|TASK_DUE_SOON"));
        // Caio: only the unassigned task of B
        verify(emailService).sendDeadlineDigest("caio@exemplo.test", "Caio", List.of("Equipe B|TASK_DUE_SOON"));
    }

    @Test
    void digest_shouldSendNothingWhenNobodyHasAlerts() {
        when(alertService.today()).thenReturn(TODAY);
        when(teamService.findAllTeams()).thenReturn(List.of(team(10L, "Equipe A")));
        when(alertService.findTeamAlerts(10L, TODAY)).thenReturn(List.of());

        assertEquals(0, digest().execute());
        verifyNoInteractions(emailService);
    }

    @Test
    void digest_shouldSkipAUserWhoseOnlyAlertsBelongToAnotherAssignee() {
        User ana = user(1, "Ana");
        when(alertService.today()).thenReturn(TODAY);
        when(teamService.findAllTeams()).thenReturn(List.of(team(10L, "Equipe A")));
        when(alertService.findTeamAlerts(10L, TODAY)).thenReturn(List.of(new TeamAlert(taskAlert(), 99L)));
        when(teamService.findMembersByTeamId(10L)).thenReturn(List.of(member(ana)));

        assertEquals(0, digest().execute());
        verifyNoInteractions(emailService);
    }
}
