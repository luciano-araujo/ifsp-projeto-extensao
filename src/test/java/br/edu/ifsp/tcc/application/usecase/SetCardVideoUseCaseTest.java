package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.dto.SetVideoDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardReviewService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.VideoStatsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetCardVideoUseCaseTest {

    private static final String URL_A = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private static final String URL_A_SHORT = "https://youtu.be/dQw4w9WgXcQ";
    private static final String URL_B = "https://youtu.be/aaaaaaaaaaa";

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private VideoStatsService videoStatsService;
    @Mock private MaterialService materialService;
    @Mock private CardReviewService cardReviewService;
    @InjectMocks private SetCardVideoUseCase useCase;

    private KanbanItem card(KanbanItemState state, String videoUrl, Long columnId) {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setState(state);
        item.setVideoUrl(videoUrl);
        if (columnId != null) {
            BoardColumn column = new BoardColumn();
            column.setId(columnId);
            item.setColumn(column);
        }
        return item;
    }

    private SetVideoDTO dto(String url) {
        SetVideoDTO dto = new SetVideoDTO();
        dto.setUrl(url);
        return dto;
    }

    private void stubResponse(KanbanItem saved, Long columnId) {
        when(kanbanItemService.save(saved)).thenReturn(saved);
        LocalDateTime since = LocalDateTime.of(2025, 1, 1, 0, 0);
        when(boardService.findColumnSince(List.of(saved))).thenReturn(Map.of(1L, since));
        when(cardReviewService.findStatuses(List.of(saved))).thenReturn(Map.of(1L, ReviewStatus.APPROVED));
        when(boardService.findTasks(saved)).thenReturn(List.of());
        when(materialService.countByCard(1L)).thenReturn(2);
        CardResponse card = mock(CardResponse.class);
        when(boardService.toCardResponse(saved, columnId, List.of(), since, 2, ReviewStatus.APPROVED)).thenReturn(card);
    }

    @Test
    void execute_shouldLinkNewVideoWithoutDeletingAnything() {
        KanbanItem item = card(KanbanItemState.IN_PRODUCTION, null, 5L);
        when(boardService.findCard(10L, 1L)).thenReturn(item);
        stubResponse(item, 5L);

        assertNotNull(useCase.execute(10L, 1L, 1L, dto("  " + URL_A + " ")));

        assertEquals(URL_A, item.getVideoUrl());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(videoStatsService, never()).deleteSnapshots(1L);
    }

    @Test
    void execute_shouldKeepHistoryWhenTheLinkPointsToTheSameVideo() {
        KanbanItem item = card(KanbanItemState.IN_PRODUCTION, URL_A, 5L);
        when(boardService.findCard(10L, 1L)).thenReturn(item);
        stubResponse(item, 5L);

        useCase.execute(10L, 1L, 1L, dto(URL_A_SHORT));

        assertEquals(URL_A_SHORT, item.getVideoUrl());
        verify(videoStatsService, never()).deleteSnapshots(1L);
    }

    @Test
    void execute_shouldClearSnapshotsWhenChangingToAnotherVideo() {
        KanbanItem item = card(KanbanItemState.IN_PRODUCTION, URL_A, 5L);
        when(boardService.findCard(10L, 1L)).thenReturn(item);
        stubResponse(item, 5L);

        useCase.execute(10L, 1L, 1L, dto(URL_B));

        assertEquals(URL_B, item.getVideoUrl());
        verify(videoStatsService).deleteSnapshots(1L);
    }

    @Test
    void execute_shouldUnlinkAndDeleteSnapshotsOnBlankOrNullUrl() {
        for (SetVideoDTO body : new SetVideoDTO[]{dto("   "), dto(null), null}) {
            KanbanItem item = card(KanbanItemState.IN_PRODUCTION, URL_A, 5L);
            when(boardService.findCard(10L, 1L)).thenReturn(item);
            stubResponse(item, 5L);

            useCase.execute(10L, 1L, 1L, body);

            assertNull(item.getVideoUrl());
        }
        verify(videoStatsService, times(3)).deleteSnapshots(1L);
    }

    @Test
    void execute_shouldFallBackToTheFirstColumnWhenCardHasNone() {
        KanbanItem item = card(KanbanItemState.IN_PRODUCTION, null, null);
        BoardColumn first = new BoardColumn();
        first.setId(9L);
        when(boardService.findCard(10L, 1L)).thenReturn(item);
        when(boardService.findColumns(10L)).thenReturn(List.of(first));
        stubResponse(item, 9L);

        assertNotNull(useCase.execute(10L, 1L, 1L, dto(URL_A)));
    }

    @Test
    void execute_shouldRejectInvalidUrlWith400() {
        KanbanItem item = card(KanbanItemState.IN_PRODUCTION, URL_A, 5L);
        when(boardService.findCard(10L, 1L)).thenReturn(item);

        for (String bad : new String[]{"https://vimeo.com/123456", "http://youtu.be/dQw4w9WgXcQ", "lixo"}) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> useCase.execute(10L, 1L, 1L, dto(bad)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals("Informe um link válido do YouTube.", ex.getReason());
        }
        assertEquals(URL_A, item.getVideoUrl());
        verify(kanbanItemService, never()).save(any());
    }

    @Test
    void execute_shouldRejectIdeaWith400() {
        when(boardService.findCard(10L, 1L)).thenReturn(card(KanbanItemState.IDEATION, null, null));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 1L, dto(URL_A)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Converta a ideia em projeto antes de vincular um vídeo.", ex.getReason());
        verify(kanbanItemService, never()).save(any());
    }

    @Test
    void execute_shouldPropagateForbiddenForNonEditors() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 1L, dto(URL_A)));
        verifyNoInteractions(boardService, kanbanItemService);
    }
}
