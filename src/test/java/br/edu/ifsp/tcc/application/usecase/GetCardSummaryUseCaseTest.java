package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardSummaryResponse;
import br.edu.ifsp.tcc.application.entity.CardSummary;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardSummaryService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCardSummaryUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private CardSummaryService cardSummaryService;
    @InjectMocks private GetCardSummaryUseCase useCase;

    @Test
    void execute_shouldReturnLastSummary() {
        CardSummary row = new CardSummary();
        CardSummaryResponse response = mock(CardSummaryResponse.class);
        when(cardSummaryService.findByCard(3L)).thenReturn(Optional.of(row));
        when(cardSummaryService.toResponse(row)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 3L));
        verify(teamService).requireMember(10L, 1L);
        verify(boardService).findCard(10L, 3L);
    }

    @Test
    void execute_shouldReturn404WhenNoSummary() {
        when(cardSummaryService.findByCard(3L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Nenhum resumo gerado para este projeto.", ex.getReason());
    }
}
