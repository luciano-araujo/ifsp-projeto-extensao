package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamMemberResponse;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListTeamMembersUseCaseTest {

    @Mock private TeamService teamService;
    @InjectMocks private ListTeamMembersUseCase useCase;

    @Test
    void execute_shouldMapMembersOfTeam() {
        TeamMember member = new TeamMember();
        TeamMemberResponse response = mock(TeamMemberResponse.class);
        when(teamService.findMembersByTeamId(10L)).thenReturn(List.of(member));
        when(teamService.toMemberResponse(member)).thenReturn(response);

        List<TeamMemberResponse> result = useCase.execute(10L, 1L);

        assertEquals(List.of(response), result);
        verify(teamService).requireMember(10L, 1L);
    }
}
