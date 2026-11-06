package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardColumnDTO;
import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateColumnUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private CreateColumnUseCase useCase;

    @Test
    void execute_shouldCreateColumnAtEndOfBoard() {
        Team team = new Team();
        team.setId(10L);
        TeamMember admin = new TeamMember();
        admin.setTeam(team);
        BoardColumnDTO dto = new BoardColumnDTO();
        dto.setName("  Publicacao ");
        BoardColumnResponse response = mock(BoardColumnResponse.class);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(boardService.countColumns(10L)).thenReturn(4);
        when(boardService.saveColumn(any(BoardColumn.class))).thenReturn(new BoardColumn());
        when(boardService.toColumnResponse(any(BoardColumn.class), eq(List.of()))).thenReturn(response);

        BoardColumnResponse result = useCase.execute(10L, 1L, dto);

        assertSame(response, result);
        verify(boardService).saveColumn(argThatNamed("Publicacao", 4));
    }

    @Test
    void execute_shouldRejectBlankName() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(new TeamMember());
        BoardColumnDTO dto = new BoardColumnDTO();
        dto.setName(" ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(boardService, never()).saveColumn(any());
    }

    private BoardColumn argThatNamed(String name, int sortOrder) {
        return org.mockito.ArgumentMatchers.argThat(column ->
                name.equals(column.getName()) && column.getSortOrder() == sortOrder);
    }
}
