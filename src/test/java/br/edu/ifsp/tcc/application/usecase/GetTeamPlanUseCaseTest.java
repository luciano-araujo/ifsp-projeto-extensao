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
class GetTeamPlanUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private PlanService planService;
    @InjectMocks private GetTeamPlanUseCase useCase;

    @Test
    void execute_shouldReturnThePlanToAnyMember() {
        Team team = new Team();
        TeamMember member = new TeamMember();
        member.setTeam(team);
        TeamPlanResponse response = mock(TeamPlanResponse.class);
        when(teamService.requireMember(10L, 1L)).thenReturn(member);
        when(planService.toPlanResponse(team)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L));
    }

    @Test
    void execute_shouldRefuseNonMembers() {
        when(teamService.requireMember(10L, 9L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não faz parte desta equipe."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 9L));
        verifyNoInteractions(planService);
    }
}
