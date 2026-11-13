package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.VideoStatsResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.VideoStatSnapshot;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import br.edu.ifsp.tcc.application.repository.VideoStatSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoStatsServiceTest {

    @Mock private VideoStatSnapshotRepository snapshotRepository;
    @Mock private KanbanItemRepository kanbanItemRepository;
    @InjectMocks private VideoStatsService service;

    private KanbanItem card(String url) {
        KanbanItem item = new KanbanItem();
        item.setId(7L);
        item.setVideoUrl(url);
        return item;
    }

    private VideoStatSnapshot snapshot(long views, LocalDateTime capturedAt) {
        VideoStatSnapshot s = new VideoStatSnapshot();
        s.setViewCount(views);
        s.setLikeCount(2L);
        s.setCommentCount(1L);
        s.setTitle("Aula " + views);
        s.setPublishedAt(LocalDateTime.of(2024, 1, 2, 3, 4, 5));
        s.setCapturedAt(capturedAt);
        return s;
    }

    @Test
    void extractVideoId_shouldAcceptEveryRn06Shape() {
        for (String url : List.of(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://youtube.com/watch?v=dQw4w9WgXcQ&t=10",
                "https://m.youtube.com/embed/dQw4w9WgXcQ",
                "https://www.youtube.com/shorts/dQw4w9WgXcQ?feature=share",
                "https://youtu.be/dQw4w9WgXcQ?si=abc",
                "HTTPS://YOUTU.BE/dQw4w9WgXcQ")) {
            assertEquals(Optional.of("dQw4w9WgXcQ"), VideoStatsService.extractVideoId(url), url);
        }
    }

    @Test
    void extractVideoId_shouldRejectAnythingElse() {
        assertTrue(VideoStatsService.extractVideoId(null).isEmpty());
        assertTrue(VideoStatsService.extractVideoId("").isEmpty());
        assertTrue(VideoStatsService.extractVideoId("http://youtu.be/dQw4w9WgXcQ").isEmpty());
        assertTrue(VideoStatsService.extractVideoId("https://vimeo.com/123456").isEmpty());
        assertTrue(VideoStatsService.extractVideoId("https://youtu.be/curto").isEmpty());
        assertTrue(VideoStatsService.extractVideoId("https://youtu.be/dQw4w9WgXcQ?x=" + "a".repeat(500)).isEmpty());
    }

    @Test
    void requireVideoId_shouldReturnIdOrThrow404() {
        assertEquals("dQw4w9WgXcQ", service.requireVideoId(card("https://youtu.be/dQw4w9WgXcQ")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.requireVideoId(card(null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Este projeto não tem vídeo do YouTube vinculado.", ex.getReason());
    }

    @Test
    void findLatest_shouldDelegate() {
        VideoStatSnapshot s = snapshot(1, LocalDateTime.now());
        when(snapshotRepository.findFirstByKanbanItem_IdAndVideoIdOrderByCapturedAtDescIdDesc(7L, "dQw4w9WgXcQ")).thenReturn(Optional.of(s));

        assertSame(s, service.findLatest(7L, "dQw4w9WgXcQ").orElseThrow());
    }

    @Test
    void isFresh_shouldCompareWithTheCacheWindow() {
        assertTrue(VideoStatsService.isFresh(snapshot(1, LocalDateTime.now().minusMinutes(5)), 15));
        assertFalse(VideoStatsService.isFresh(snapshot(1, LocalDateTime.now().minusMinutes(16)), 15));
    }

    @Test
    void findCardsWithVideo_shouldDelegate() {
        List<KanbanItem> cards = List.of(card("https://youtu.be/dQw4w9WgXcQ"));
        when(kanbanItemRepository.findByVideoUrlIsNotNull()).thenReturn(cards);

        assertSame(cards, service.findCardsWithVideo());
    }

    @Test
    void deleteSnapshots_shouldDelegate() {
        service.deleteSnapshots(7L);

        verify(snapshotRepository).deleteByCardId(7L);
    }

    @Test
    void saveSnapshot_shouldTruncateTimestampsAndLongTitle() {
        when(snapshotRepository.save(any(VideoStatSnapshot.class))).thenAnswer(i -> i.getArgument(0));
        KanbanItem item = card("https://youtu.be/dQw4w9WgXcQ");
        VideoStats stats = new VideoStats("dQw4w9WgXcQ", 100, null, 5L, "t".repeat(300),
                LocalDateTime.of(2024, 1, 2, 3, 4, 5, 999_000_000));

        service.saveSnapshot(item, stats);

        ArgumentCaptor<VideoStatSnapshot> captor = ArgumentCaptor.forClass(VideoStatSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        VideoStatSnapshot saved = captor.getValue();
        assertSame(item, saved.getKanbanItem());
        assertEquals(7L, saved.getKanbanItemId());
        assertEquals("dQw4w9WgXcQ", saved.getVideoId());
        assertEquals(100L, saved.getViewCount());
        assertNull(saved.getLikeCount());
        assertEquals(5L, saved.getCommentCount());
        assertEquals(255, saved.getTitle().length());
        assertEquals(0, saved.getPublishedAt().getNano());
        assertEquals(0, saved.getCapturedAt().getNano());
    }

    @Test
    void saveSnapshot_shouldAcceptMissingTitleAndPublishedAt() {
        when(snapshotRepository.save(any(VideoStatSnapshot.class))).thenAnswer(i -> i.getArgument(0));

        VideoStatSnapshot saved = service.saveSnapshot(card("https://youtu.be/dQw4w9WgXcQ"),
                new VideoStats("dQw4w9WgXcQ", 1, null, null, null, null));

        assertNull(saved.getTitle());
        assertNull(saved.getPublishedAt());
    }

    @Test
    void toResponse_shouldReturnHistoryOldestFirstAndLatestFromTheNewest() {
        LocalDateTime now = LocalDateTime.of(2025, 5, 1, 12, 0, 0);
        when(snapshotRepository.findTop60ByKanbanItem_IdAndVideoIdOrderByCapturedAtDescIdDesc(7L, "dQw4w9WgXcQ"))
                .thenReturn(List.of(snapshot(30, now), snapshot(20, now.minusDays(1)), snapshot(10, now.minusDays(2))));

        VideoStatsResponse response = service.toResponse(card("https://youtu.be/dQw4w9WgXcQ"));

        assertEquals("dQw4w9WgXcQ", response.getVideoId());
        assertEquals("https://youtu.be/dQw4w9WgXcQ", response.getVideoUrl());
        assertEquals("Aula 30", response.getTitle());
        assertEquals(LocalDateTime.of(2024, 1, 2, 3, 4, 5), response.getPublishedAt());
        assertEquals(30L, response.getLatest().getViewCount());
        assertEquals(now, response.getLatest().getCapturedAt());
        assertEquals(List.of(10L, 20L, 30L), response.getHistory().stream().map(h -> h.getViewCount()).toList());
    }

    @Test
    void toResponse_shouldHaveNullLatestAndEmptyHistoryBeforeTheFirstRefresh() {
        when(snapshotRepository.findTop60ByKanbanItem_IdAndVideoIdOrderByCapturedAtDescIdDesc(7L, "dQw4w9WgXcQ")).thenReturn(List.of());

        VideoStatsResponse response = service.toResponse(card("https://youtu.be/dQw4w9WgXcQ"));

        assertNull(response.getLatest());
        assertNull(response.getTitle());
        assertNull(response.getPublishedAt());
        assertTrue(response.getHistory().isEmpty());
    }

    @Test
    void toResponse_shouldThrow404WithoutLink() {
        assertThrows(ResponseStatusException.class, () -> service.toResponse(card(null)));
    }
}
