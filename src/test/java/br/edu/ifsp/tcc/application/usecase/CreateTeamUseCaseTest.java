package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateTeamDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateTeamUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private PlanService planService;
    @InjectMocks private CreateTeamUseCase useCase;

    @Test
    void execute_shouldCreateTeamWithTrimmedNameAndReturnResponse() {
        User user = new User();
        CreateTeamDTO dto = new CreateTeamDTO();
        dto.setName("  Equipe Office ");
        TeamMember admin = new TeamMember();
        TeamResponse response = mock(TeamResponse.class);
        when(teamService.createTeamWithAdmin(user, "Equipe Office")).thenReturn(admin);
        when(teamService.toTeamResponse(admin)).thenReturn(response);

        TeamResponse result = useCase.execute(user, dto);

        assertSame(response, result);
    }


    @Test
    void execute_shouldStopWith402WhenTheUserAlreadyAdministersAFreeTeam() {
        User user = new User();
        user.setId(7L);
        CreateTeamDTO dto = new CreateTeamDTO();
        dto.setName("Outra");
        doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "limite")).when(planService).assertCanCreateFreeTeam(7L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(user, dto));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
        verifyNoInteractions(teamService);
    }
}
