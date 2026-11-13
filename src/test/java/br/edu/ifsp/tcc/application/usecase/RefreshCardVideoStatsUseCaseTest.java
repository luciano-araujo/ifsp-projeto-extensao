package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.VideoStatsResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.VideoStatSnapshot;
import br.edu.ifsp.tcc.application.exception.TooManyRequestsException;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.RateLimitService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.VideoStats;
import br.edu.ifsp.tcc.application.service.VideoStatsService;
import br.edu.ifsp.tcc.application.service.YouTubeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshCardVideoStatsUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private VideoStatsService videoStatsService;
    @Mock private YouTubeClient youTubeClient;
    @Mock private RateLimitService rateLimitService;
    @InjectMocks private RefreshCardVideoStatsUseCase useCase;

    private KanbanItem item;
    private final VideoStatsResponse response = mock(VideoStatsResponse.class);

    @BeforeEach
    void setUp() {
        item = new KanbanItem();
        item.setId(3L);
        when(boardService.findCard(10L, 3L)).thenReturn(item);
        when(videoStatsService.requireVideoId(item)).thenReturn("dQw4w9WgXcQ");
    }

    private VideoStatSnapshot snapshot(LocalDateTime capturedAt) {
        VideoStatSnapshot s = new VideoStatSnapshot();
        s.setCapturedAt(capturedAt);
        return s;
    }

    @Test
    void execute_shouldCallYouTubeSaveAndReturnWhenThereIsNoSnapshot() {
        VideoStats stats = new VideoStats("dQw4w9WgXcQ", 5, 1L, 0L, "t", null);
        when(videoStatsService.findLatest(3L, "dQw4w9WgXcQ")).thenReturn(Optional.empty());
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(youTubeClient.fetch(List.of("dQw4w9WgXcQ"))).thenReturn(Map.of("dQw4w9WgXcQ", stats));
        when(videoStatsService.toResponse(item)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 3L));

        verify(teamService).requireMember(10L, 1L);
        verify(rateLimitService).registerAttempt("youtube:10", 30, Duration.ofHours(1));
        verify(videoStatsService).saveSnapshot(item, stats);
    }

    @Test
    void execute_shouldRefreshWhenTheLastSnapshotIsOlderThanTheCacheWindow() {
        VideoStats stats = new VideoStats("dQw4w9WgXcQ", 5, null, null, null, null);
        when(videoStatsService.findLatest(3L, "dQw4w9WgXcQ")).thenReturn(Optional.of(snapshot(LocalDateTime.now().minusMinutes(16))));
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(youTubeClient.fetch(List.of("dQw4w9WgXcQ"))).thenReturn(Map.of("dQw4w9WgXcQ", stats));
        when(videoStatsService.toResponse(item)).thenReturn(response);

        useCase.execute(10L, 1L, 3L);

        verify(videoStatsService).saveSnapshot(item, stats);
    }

    @Test
    void execute_shouldReturnCachedWithoutCallingYouTubeWhenSnapshotIsYoung() {
        when(videoStatsService.findLatest(3L, "dQw4w9WgXcQ")).thenReturn(Optional.of(snapshot(LocalDateTime.now().minusMinutes(2))));
        when(videoStatsService.toResponse(item)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 3L));

        verifyNoInteractions(youTubeClient);
        verify(videoStatsService, never()).saveSnapshot(any(), any());
    }

    @Test
    void execute_shouldAnswer503WhenNoKeyIsConfigured() {
        when(videoStatsService.findLatest(3L, "dQw4w9WgXcQ")).thenReturn(Optional.empty());
        when(youTubeClient.isConfigured()).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        assertEquals("Integração com o YouTube não configurada.", ex.getReason());
        verify(youTubeClient, never()).fetch(any());
    }

    @Test
    void execute_shouldAnswer429AndNotCallYouTubeWhenTheTeamLimitIsReached() {
        when(videoStatsService.findLatest(3L, "dQw4w9WgXcQ")).thenReturn(Optional.empty());
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(rateLimitService.registerAttempt("youtube:10", 30, Duration.ofHours(1))).thenReturn(900L);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class, () -> useCase.execute(10L, 1L, 3L));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        assertEquals(900L, ex.getRetryAfterSeconds());
        assertEquals("Limite de atualizações do YouTube atingido para esta equipe. Tente novamente mais tarde.", ex.getReason());
        verify(youTubeClient, never()).fetch(any());
    }

    @Test
    void execute_shouldNotCountTheAttemptOnCacheHitOrWithoutKey() {
        when(videoStatsService.findLatest(3L, "dQw4w9WgXcQ")).thenReturn(Optional.of(snapshot(LocalDateTime.now().minusMinutes(1))));
        when(videoStatsService.toResponse(item)).thenReturn(response);

        useCase.execute(10L, 1L, 3L);

        verifyNoInteractions(rateLimitService);
    }

    @Test
    void execute_shouldAnswer404WhenVideoIsNotFound() {
        when(videoStatsService.findLatest(3L, "dQw4w9WgXcQ")).thenReturn(Optional.empty());
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(youTubeClient.fetch(List.of("dQw4w9WgXcQ"))).thenReturn(Map.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Vídeo não encontrado ou privado no YouTube.", ex.getReason());
        verify(videoStatsService, never()).saveSnapshot(any(), any());
    }

    @Test
    void execute_shouldPropagateYouTubeFailureAndSaveNothing() {
        when(videoStatsService.findLatest(3L, "dQw4w9WgXcQ")).thenReturn(Optional.empty());
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(youTubeClient.fetch(any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        verify(videoStatsService, never()).saveSnapshot(any(), any());
    }
}
