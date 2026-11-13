package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.VideoStats;
import br.edu.ifsp.tcc.application.service.VideoStatsService;
import br.edu.ifsp.tcc.application.service.YouTubeClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshAllVideoStatsUseCaseTest {

    @Mock private VideoStatsService videoStatsService;
    @Mock private YouTubeClient youTubeClient;
    @InjectMocks private RefreshAllVideoStatsUseCase useCase;

    private static String id(int n) {
        return String.format("v%010d", n);
    }

    private KanbanItem card(long cardId, String videoId) {
        KanbanItem item = new KanbanItem();
        item.setId(cardId);
        item.setVideoUrl("https://youtu.be/" + videoId);
        return item;
    }

    private VideoStats stats(String videoId) {
        return new VideoStats(videoId, 1, null, null, "t", null);
    }

    @Test
    void execute_shouldDoNothingWithoutApiKey() {
        when(youTubeClient.isConfigured()).thenReturn(false);

        assertEquals(0, useCase.execute());

        verifyNoInteractions(videoStatsService);
        verify(youTubeClient, never()).fetch(any());
    }

    @Test
    void execute_shouldDoNothingWhenNoCardHasAVideo() {
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(videoStatsService.findCardsWithVideo()).thenReturn(List.of());

        assertEquals(0, useCase.execute());

        verify(youTubeClient, never()).fetch(any());
    }

    @Test
    void execute_shouldBatchAtMost50IdsPerCallAndSaveOneSnapshotPerCard() {
        List<KanbanItem> cards = new ArrayList<>();
        Map<String, VideoStats> all = new HashMap<>();
        for (int i = 0; i < 120; i++) {
            cards.add(card(i + 1, id(i)));
            all.put(id(i), stats(id(i)));
        }
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(videoStatsService.findCardsWithVideo()).thenReturn(cards);
        when(youTubeClient.fetch(any())).thenAnswer(inv -> {
            Map<String, VideoStats> part = new HashMap<>();
            ((List<String>) inv.getArgument(0)).forEach(v -> part.put(v, all.get(v)));
            return part;
        });

        assertEquals(120, useCase.execute());

        ArgumentCaptor<List<String>> batches = ArgumentCaptor.forClass(List.class);
        verify(youTubeClient, times(3)).fetch(batches.capture());
        assertEquals(List.of(50, 50, 20), batches.getAllValues().stream().map(List::size).toList());
        verify(videoStatsService, times(120)).saveSnapshot(any(), any());
    }

    @Test
    void execute_shouldSaveForEveryCardThatSharesAVideoAndSkipInvalidLinks() {
        KanbanItem a = card(1, "dQw4w9WgXcQ");
        KanbanItem b = card(2, "dQw4w9WgXcQ");
        KanbanItem broken = new KanbanItem();
        broken.setId(3L);
        broken.setVideoUrl("https://example.com/x");
        VideoStats s = stats("dQw4w9WgXcQ");
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(videoStatsService.findCardsWithVideo()).thenReturn(List.of(a, b, broken));
        when(youTubeClient.fetch(List.of("dQw4w9WgXcQ"))).thenReturn(Map.of("dQw4w9WgXcQ", s));

        assertEquals(2, useCase.execute());

        verify(videoStatsService).saveSnapshot(a, s);
        verify(videoStatsService).saveSnapshot(b, s);
    }

    @Test
    void execute_shouldSkipVideosYouTubeDidNotReturn() {
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(videoStatsService.findCardsWithVideo()).thenReturn(List.of(card(1, "dQw4w9WgXcQ")));
        when(youTubeClient.fetch(any())).thenReturn(Map.of());

        assertEquals(0, useCase.execute());

        verify(videoStatsService, never()).saveSnapshot(any(), any());
    }

    @Test
    void execute_shouldGoOnAfterAFailedBatchAndAFailedSave() {
        List<KanbanItem> cards = new ArrayList<>();
        for (int i = 0; i < 52; i++) cards.add(card(i + 1, id(i)));
        KanbanItem failing = cards.get(50);
        VideoStats ok = stats(id(51));
        when(youTubeClient.isConfigured()).thenReturn(true);
        when(videoStatsService.findCardsWithVideo()).thenReturn(cards);
        // first batch (ids 0..49) fails, second (50, 51) works
        when(youTubeClient.fetch(any())).thenAnswer(inv -> {
            List<String> batch = inv.getArgument(0);
            if (batch.size() == 50) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            return Map.of(id(50), stats(id(50)), id(51), ok);
        });
        doThrow(new IllegalStateException("db")).doReturn(null).when(videoStatsService).saveSnapshot(any(), any());

        assertEquals(1, useCase.execute());

        verify(videoStatsService).saveSnapshot(cards.get(51), ok);
    }
}
