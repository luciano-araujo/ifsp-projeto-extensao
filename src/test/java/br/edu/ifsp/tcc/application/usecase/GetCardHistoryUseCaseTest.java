package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardMovementResponse;
import br.edu.ifsp.tcc.application.entity.CardMovement;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCardHistoryUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private GetCardHistoryUseCase useCase;

    @Test
    void execute_shouldReturnHistoryNewestFirstForAnyMember() {
        CardMovement newest = new CardMovement();
        CardMovement oldest = new CardMovement();
        CardMovementResponse r1 = mock(CardMovementResponse.class);
        CardMovementResponse r2 = mock(CardMovementResponse.class);
        when(boardService.findHistory(14L)).thenReturn(List.of(newest, oldest));
        when(boardService.toMovementResponse(newest)).thenReturn(r1);
        when(boardService.toMovementResponse(oldest)).thenReturn(r2);

        List<CardMovementResponse> result = useCase.execute(10L, 1L, 14L);

        assertEquals(List.of(r1, r2), result);
        verify(teamService).requireMember(10L, 1L);
        verify(boardService).findCard(10L, 14L);
    }

    @Test
    void execute_shouldReturnNotFoundForCardOfAnotherTeam() {
        when(boardService.findCard(10L, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Card não encontrado."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(boardService, never()).findHistory(99L);
    }

    @Test
    void execute_shouldBlockNonMembers() {
        when(teamService.requireMember(10L, 9L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não faz parte desta equipe."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 9L, 14L));
        verifyNoInteractions(boardService);
    }
}
