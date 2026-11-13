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
class ListPlansUseCaseTest {

    @Mock private PlanService planService;
    @InjectMocks private ListPlansUseCase useCase;

    @Test
    void execute_shouldReturnTheCatalogue() {
        List<PlanResponse> plans = List.of(mock(PlanResponse.class));
        when(planService.listPlans()).thenReturn(plans);

        assertSame(plans, useCase.execute());
    }
}
