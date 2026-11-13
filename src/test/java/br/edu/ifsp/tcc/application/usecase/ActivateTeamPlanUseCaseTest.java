package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.SystemAdminService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivateTeamPlanUseCaseTest {

    @Mock private SystemAdminService systemAdminService;
    @Mock private PlanService planService;
    @InjectMocks private ActivateTeamPlanUseCase useCase;

    @Test
    void execute_shouldActivateThePlanForTheSystemAdmin() {
        User admin = new User();
        Team team = new Team();
        LocalDateTime until = LocalDateTime.of(2027, 1, 1, 0, 0);
        ActivatePlanDTO dto = new ActivatePlanDTO();
        dto.setPlan(PlanType.AGENCIA);
        dto.setExpiresAt(until);
        TeamPlanResponse response = mock(TeamPlanResponse.class);
        when(planService.findTeam(10L)).thenReturn(team);
        when(planService.activate(team, PlanType.AGENCIA, until)).thenReturn(team);
        when(planService.toTeamPlanResponse(team)).thenReturn(response);

        assertSame(response, useCase.execute(admin, 10L, dto));
        verify(systemAdminService).requireSystemAdmin(admin);
    }

    @Test
    void execute_shouldRefuseWhoIsNotSystemAdmin() {
        User user = new User();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "negado")).when(systemAdminService).requireSystemAdmin(user);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(user, 10L, new ActivatePlanDTO()));
        verifyNoInteractions(planService);
    }
}
