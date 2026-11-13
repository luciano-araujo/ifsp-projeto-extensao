package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.dto.AlertType;
import br.edu.ifsp.tcc.application.service.AlertService;
import br.edu.ifsp.tcc.application.service.AlertService.TeamAlert;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListAlertsUseCaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    @Mock private TeamService teamService;
    @Mock private AlertService alertService;
    @InjectMocks private ListAlertsUseCase useCase;

    @Test
    void execute_shouldReturnTheAlertsOfTheTeamForAnyMember() {
        AlertResponse card = new AlertResponse(AlertType.CARD_OVERDUE, 1L, "Aula 1", null, null, TODAY.minusDays(1), -1);
        AlertResponse task = new AlertResponse(AlertType.TASK_DUE_SOON, 1L, "Aula 1", 7L, "Revisar", TODAY.plusDays(2), 2);
        when(alertService.today()).thenReturn(TODAY);
        when(alertService.findTeamAlerts(10L, TODAY)).thenReturn(List.of(new TeamAlert(card, null), new TeamAlert(task, 3L)));

        assertEquals(List.of(card, task), useCase.execute(10L, 1L));
        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void execute_shouldPropagateForbiddenForNonMembers() {
        when(teamService.requireMember(10L, 9L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "x"));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 9L));
        verifyNoInteractions(alertService);
    }
}
