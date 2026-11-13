package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.dto.AlertType;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.AlertService;
import br.edu.ifsp.tcc.application.service.AlertService.DigestAlert;
import br.edu.ifsp.tcc.application.service.AlertService.TeamAlert;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendDeadlineDigestUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @Mock private TeamService teamService;
    @Mock private AlertService alertService;
    @Mock private EmailService emailService;
    @InjectMocks private SendDeadlineDigestUseCase useCase;

    private User user(long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@exemplo.test");
        return user;
    }

    private TeamMember member(long teamId, User user) {
        Team team = new Team();
        team.setId(teamId);
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setRole(TeamRole.CREATOR);
        return member;
    }

    private AlertResponse cardAlert() {
        return new AlertResponse(AlertType.CARD_OVERDUE, 1L, "Aula 1", null, null, TODAY.minusDays(1), -1);
    }

    private AlertResponse taskAlert() {
        return new AlertResponse(AlertType.TASK_DUE_SOON, 1L, "Aula 1", 7L, "Revisar", TODAY.plusDays(2), 2);
    }

    @Test
    void execute_shouldSendOneEmailPerUserCoveringAllTheirTeams() {
        User ana = user(1, "Ana");
        User bia = user(2, "Bia");
        User caio = user(3, "Caio");
        when(alertService.today()).thenReturn(TODAY);
        // team A: a card alert (everyone) + a task assigned to Bia; team B: an unassigned task
        when(alertService.findAllAlerts(TODAY)).thenReturn(List.of(
                new DigestAlert(10L, "Equipe A", new TeamAlert(cardAlert(), null)),
                new DigestAlert(10L, "Equipe A", new TeamAlert(taskAlert(), 2L)),
                new DigestAlert(20L, "Equipe B", new TeamAlert(taskAlert(), null))));
        when(teamService.findMembersOfTeams(any())).thenReturn(List.of(
                member(10L, ana), member(10L, bia), member(20L, ana), member(20L, caio)));
        when(alertService.describe(anyString(), any(AlertResponse.class)))
                .thenAnswer(inv -> inv.getArgument(0) + "|" + ((AlertResponse) inv.getArgument(1)).getType());

        int sent = useCase.execute();

        assertEquals(3, sent);
        verify(emailService).sendDeadlineDigest("ana@exemplo.test", "Ana",
                List.of("Equipe A|CARD_OVERDUE", "Equipe B|TASK_DUE_SOON"));
        verify(emailService).sendDeadlineDigest("bia@exemplo.test", "Bia",
                List.of("Equipe A|CARD_OVERDUE", "Equipe A|TASK_DUE_SOON"));
        verify(emailService).sendDeadlineDigest("caio@exemplo.test", "Caio", List.of("Equipe B|TASK_DUE_SOON"));
    }

    @Test
    void execute_shouldLoadTheMembersOfTheTeamsWithAlertsInOneCall() {
        when(alertService.today()).thenReturn(TODAY);
        when(alertService.findAllAlerts(TODAY)).thenReturn(List.of(
                new DigestAlert(10L, "A", new TeamAlert(cardAlert(), null)),
                new DigestAlert(10L, "A", new TeamAlert(taskAlert(), null)),
                new DigestAlert(20L, "B", new TeamAlert(cardAlert(), null))));
        when(teamService.findMembersOfTeams(any())).thenReturn(List.of());

        assertEquals(0, useCase.execute());

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Collection<Long>> ids = org.mockito.ArgumentCaptor.forClass(Collection.class);
        verify(teamService, times(1)).findMembersOfTeams(ids.capture());
        assertEquals(List.of(10L, 20L), List.copyOf(ids.getValue()));
        verifyNoInteractions(emailService);
    }

    @Test
    void execute_shouldDoNothingWhenThereAreNoAlerts() {
        when(alertService.today()).thenReturn(TODAY);
        when(alertService.findAllAlerts(TODAY)).thenReturn(List.of());

        assertEquals(0, useCase.execute());
        verifyNoInteractions(teamService, emailService);
    }

    @Test
    void execute_shouldSkipAUserWhoseOnlyAlertsBelongToAnotherAssignee() {
        when(alertService.today()).thenReturn(TODAY);
        when(alertService.findAllAlerts(TODAY)).thenReturn(List.of(
                new DigestAlert(10L, "A", new TeamAlert(taskAlert(), 99L))));
        when(teamService.findMembersOfTeams(any())).thenReturn(List.of(member(10L, user(1, "Ana"))));

        assertEquals(0, useCase.execute());
        verifyNoInteractions(emailService);
    }
}
