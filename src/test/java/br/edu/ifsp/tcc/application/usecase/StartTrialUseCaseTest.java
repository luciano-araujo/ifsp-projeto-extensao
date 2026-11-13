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
class StartTrialUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private PlanService planService;
    @InjectMocks private StartTrialUseCase useCase;

    private StartTrialDTO dto(PlanType plan) {
        StartTrialDTO dto = new StartTrialDTO();
        dto.setPlan(plan);
        return dto;
    }

    @Test
    void execute_shouldStartTheTrialForTheTeamAdmin() {
        Team team = new Team();
        TeamMember admin = new TeamMember();
        admin.setTeam(team);
        TeamPlanResponse response = mock(TeamPlanResponse.class);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(planService.startTrial(team, PlanType.PRO)).thenReturn(team);
        when(planService.toTeamPlanResponse(team)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, dto(PlanType.PRO)));
    }

    @Test
    void execute_shouldRefuseNonAdmins() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, dto(PlanType.PRO)));
        verifyNoInteractions(planService);
    }
}
