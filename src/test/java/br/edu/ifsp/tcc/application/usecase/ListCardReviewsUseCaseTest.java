package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardReviewResponse;
import br.edu.ifsp.tcc.application.entity.CardReview;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardReviewService;
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
class ListCardReviewsUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private CardReviewService cardReviewService;
    @InjectMocks private ListCardReviewsUseCase useCase;

    @Test
    void execute_shouldMapReviewsInServiceOrder() {
        CardReview newer = new CardReview();
        CardReview older = new CardReview();
        CardReviewResponse newerResponse = mock(CardReviewResponse.class);
        CardReviewResponse olderResponse = mock(CardReviewResponse.class);
        when(cardReviewService.findByCard(3L)).thenReturn(List.of(newer, older));
        when(cardReviewService.toResponse(newer)).thenReturn(newerResponse);
        when(cardReviewService.toResponse(older)).thenReturn(olderResponse);

        List<CardReviewResponse> result = useCase.execute(10L, 1L, 3L);

        assertEquals(List.of(newerResponse, olderResponse), result);
        verify(teamService).requireMember(10L, 1L);
        verify(boardService).findCard(10L, 3L);
    }

    @Test
    void execute_shouldFailWhenCardIsNotOfTheTeam() {
        when(boardService.findCard(10L, 3L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        verifyNoInteractions(cardReviewService);
    }
}
