package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.usecase.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private ActivateTeamPlanUseCase activateTeamPlanUseCase;
    @Mock private ListTeamsByAdminEmailUseCase listTeamsByAdminEmailUseCase;
    @InjectMocks private AdminController controller;

    @Test
    void listTeams_shouldReturnTheTeamsOfTheEmail() {
        User user = new User();
        List<AdminTeamResponse> teams = List.of(mock(AdminTeamResponse.class));
        when(listTeamsByAdminEmailUseCase.execute(user, "ana@test.com")).thenReturn(teams);

        ResponseEntity<List<AdminTeamResponse>> response = controller.listTeams(user, "ana@test.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(teams, response.getBody());
    }

    @Test
    void activatePlan_shouldReturnThePlanInForce() {
        User user = new User();
        ActivatePlanDTO dto = new ActivatePlanDTO();
        TeamPlanResponse plan = mock(TeamPlanResponse.class);
        when(activateTeamPlanUseCase.execute(user, 10L, dto)).thenReturn(plan);

        assertSame(plan, controller.activatePlan(user, 10L, dto).getBody());
    }
}
