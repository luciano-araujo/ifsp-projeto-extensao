package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.VideoStatsResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.VideoStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCardVideoStatsUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private VideoStatsService videoStatsService;
    @InjectMocks private GetCardVideoStatsUseCase useCase;

    @Test
    void execute_shouldReturnStatsForAnyMember() {
        KanbanItem item = new KanbanItem();
        VideoStatsResponse response = mock(VideoStatsResponse.class);
        when(boardService.findCard(10L, 3L)).thenReturn(item);
        when(videoStatsService.toResponse(item)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 3L));

        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void execute_shouldPropagate404WhenCardHasNoVideo() {
        KanbanItem item = new KanbanItem();
        when(boardService.findCard(10L, 3L)).thenReturn(item);
        when(videoStatsService.toResponse(item)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
    }

    @Test
    void execute_shouldPropagateNonMember() {
        when(teamService.requireMember(10L, 1L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        verifyNoInteractions(boardService, videoStatsService);
    }
}
