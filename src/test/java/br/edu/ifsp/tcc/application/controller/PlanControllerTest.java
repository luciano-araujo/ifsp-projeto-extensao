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
class PlanControllerTest {

    @Mock private ListPlansUseCase listPlansUseCase;
    @Mock private GetTeamPlanUseCase getTeamPlanUseCase;
    @Mock private StartTrialUseCase startTrialUseCase;
    @InjectMocks private PlanController controller;

    @Test
    void list_shouldReturnThePlans() {
        List<PlanResponse> plans = List.of(mock(PlanResponse.class));
        when(listPlansUseCase.execute()).thenReturn(plans);

        ResponseEntity<List<PlanResponse>> response = controller.list();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(plans, response.getBody());
    }

    @Test
    void get_shouldReturnThePlanOfTheTeam() {
        User user = new User();
        user.setId(1L);
        TeamPlanResponse plan = mock(TeamPlanResponse.class);
        when(getTeamPlanUseCase.execute(10L, 1L)).thenReturn(plan);

        assertSame(plan, controller.get(user, 10L).getBody());
    }

    @Test
    void startTrial_shouldReturnThePlanInForce() {
        User user = new User();
        user.setId(1L);
        StartTrialDTO dto = new StartTrialDTO();
        TeamPlanResponse plan = mock(TeamPlanResponse.class);
        when(startTrialUseCase.execute(10L, 1L, dto)).thenReturn(plan);

        ResponseEntity<TeamPlanResponse> response = controller.startTrial(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(plan, response.getBody());
    }
}
