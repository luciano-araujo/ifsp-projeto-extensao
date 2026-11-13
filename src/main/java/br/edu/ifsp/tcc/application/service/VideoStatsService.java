package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.VideoHistoryPointResponse;
import br.edu.ifsp.tcc.application.dto.VideoLatestResponse;
import br.edu.ifsp.tcc.application.dto.VideoStatsResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.VideoStatSnapshot;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import br.edu.ifsp.tcc.application.repository.VideoStatSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Data access, YouTube id extraction and mapping of the video statistics of a card. Business rules live in the use cases. */
@Service
public class VideoStatsService {

    public static final String INVALID_URL = "Informe um link válido do YouTube.";
    public static final String NO_VIDEO = "Este projeto não tem vídeo do YouTube vinculado.";
    public static final String NOT_CONFIGURED = "Integração com o YouTube não configurada.";
    static final int MAX_URL_LENGTH = 500;
    static final int MAX_TITLE_LENGTH = 255;

    // Same link shape as RN06 (CoursePublicationService.YOUTUBE); this one only locates the 11-character id
    private static final Pattern VIDEO_ID = Pattern.compile(
            "^https://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})",
            Pattern.CASE_INSENSITIVE);

    private final VideoStatSnapshotRepository snapshotRepository;
    private final KanbanItemRepository kanbanItemRepository;

    public VideoStatsService(VideoStatSnapshotRepository snapshotRepository, KanbanItemRepository kanbanItemRepository) {
        this.snapshotRepository = snapshotRepository;
        this.kanbanItemRepository = kanbanItemRepository;
    }

    /** The video id of a link that passes the RN06 YouTube pattern; empty otherwise (also for null and blank). */
    public static Optional<String> extractVideoId(String url) {
        if (url == null || url.length() > MAX_URL_LENGTH || !CoursePublicationService.YOUTUBE.matcher(url).matches()) {
            return Optional.empty();
        }
        Matcher matcher = VIDEO_ID.matcher(url);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** The id of the video linked to the card; 404 when there is no link. */
    public String requireVideoId(KanbanItem item) {
        return extractVideoId(item.getVideoUrl())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NO_VIDEO));
    }

    public Optional<VideoStatSnapshot> findLatest(Long cardId, String videoId) {
        return snapshotRepository.findFirstByKanbanItem_IdAndVideoIdOrderByCapturedAtDescIdDesc(cardId, videoId);
    }

    public static boolean isFresh(VideoStatSnapshot snapshot, long cacheMinutes) {
        return snapshot.getCapturedAt().isAfter(LocalDateTime.now().minusMinutes(cacheMinutes));
    }

    public List<KanbanItem> findCardsWithVideo() {
        return kanbanItemRepository.findByVideoUrlIsNotNull();
    }

    @Transactional
    public void deleteSnapshots(Long cardId) {
        snapshotRepository.deleteByCardId(cardId);
    }

    @Transactional
    public VideoStatSnapshot saveSnapshot(KanbanItem item, VideoStats stats) {
        VideoStatSnapshot snapshot = new VideoStatSnapshot();
        snapshot.setKanbanItem(item);
        snapshot.setVideoId(stats.videoId());
        snapshot.setViewCount(stats.viewCount());
        snapshot.setLikeCount(stats.likeCount());
        snapshot.setCommentCount(stats.commentCount());
        String title = stats.title();
        snapshot.setTitle(title != null && title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title);
        snapshot.setPublishedAt(stats.publishedAt() == null ? null : stats.publishedAt().truncatedTo(ChronoUnit.SECONDS));
        snapshot.setCapturedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return snapshotRepository.save(snapshot);
    }

    /** The response of GET/refresh: latest snapshot plus the history (oldest first, at most 60). */
    public VideoStatsResponse toResponse(KanbanItem item) {
        String videoId = requireVideoId(item);
        List<VideoStatSnapshot> recentFirst = snapshotRepository.findTop60ByKanbanItem_IdAndVideoIdOrderByCapturedAtDescIdDesc(item.getId(), videoId);
        VideoStatSnapshot latest = recentFirst.isEmpty() ? null : recentFirst.get(0);
        List<VideoStatSnapshot> oldestFirst = new ArrayList<>(recentFirst);
        Collections.reverse(oldestFirst);
        List<VideoHistoryPointResponse> history = oldestFirst.stream()
                .map(s -> new VideoHistoryPointResponse(s.getCapturedAt(), s.getViewCount(), s.getLikeCount(), s.getCommentCount()))
                .toList();
        return new VideoStatsResponse(videoId, item.getVideoUrl(),
                latest == null ? null : latest.getTitle(), latest == null ? null : latest.getPublishedAt(),
                latest == null ? null : new VideoLatestResponse(latest.getViewCount(), latest.getLikeCount(),
                        latest.getCommentCount(), latest.getCapturedAt()),
                history);
    }
}
