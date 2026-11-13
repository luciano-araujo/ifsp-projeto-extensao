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
class ListTeamsByAdminEmailUseCaseTest {

    @Mock private SystemAdminService systemAdminService;
    @Mock private UserService userService;
    @Mock private PlanService planService;
    @InjectMocks private ListTeamsByAdminEmailUseCase useCase;

    @Test
    void execute_shouldListTheTeamsAdministeredByTheEmail() {
        User admin = new User();
        User target = new User();
        target.setId(5L);
        Team team = new Team();
        AdminTeamResponse mapped = mock(AdminTeamResponse.class);
        when(userService.findByEmail("ana@test.com")).thenReturn(Optional.of(target));
        when(planService.findTeamsAdministeredBy(5L)).thenReturn(List.of(team));
        when(planService.toAdminTeamResponse(team)).thenReturn(mapped);

        assertEquals(List.of(mapped), useCase.execute(admin, " ana@test.com "));
        verify(systemAdminService).requireSystemAdmin(admin);
    }

    @Test
    void execute_shouldReturnEmptyWhenNobodyHasTheEmail() {
        when(userService.findByEmail("x@test.com")).thenReturn(Optional.empty());

        assertTrue(useCase.execute(new User(), "x@test.com").isEmpty());
        verifyNoInteractions(planService);
    }

    @Test
    void execute_shouldRefuseWhoIsNotSystemAdmin() {
        User user = new User();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "negado")).when(systemAdminService).requireSystemAdmin(user);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(user, "x@test.com"));
        verifyNoInteractions(userService, planService);
    }
}
