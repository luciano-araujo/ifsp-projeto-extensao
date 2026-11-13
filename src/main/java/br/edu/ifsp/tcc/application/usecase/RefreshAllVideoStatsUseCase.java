package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.VideoStats;
import br.edu.ifsp.tcc.application.service.VideoStatsService;
import br.edu.ifsp.tcc.application.service.YouTubeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RefreshAllVideoStatsUseCase {

    static final int BATCH_SIZE = 50;

    private static final Logger log = LoggerFactory.getLogger(RefreshAllVideoStatsUseCase.class);

    private final VideoStatsService videoStatsService;
    private final YouTubeClient youTubeClient;

    public RefreshAllVideoStatsUseCase(VideoStatsService videoStatsService, YouTubeClient youTubeClient) {
        this.videoStatsService = videoStatsService;
        this.youTubeClient = youTubeClient;
    }

    /**
     * Daily job: one snapshot for every card with a video link, up to 50 ids per YouTube call. Does nothing without an
     * API key. A failed batch or card is logged and skipped; the rest goes on. Returns how many snapshots were saved.
     * Deliberately not transactional: each snapshot is saved in its own short transaction.
     */
    public int execute() {
        if (!youTubeClient.isConfigured()) {
            log.info("YouTube refresh skipped: no API key configured.");
            return 0;
        }
        Map<String, List<KanbanItem>> cardsByVideo = new LinkedHashMap<>();
        for (KanbanItem item : videoStatsService.findCardsWithVideo()) {
            VideoStatsService.extractVideoId(item.getVideoUrl())
                    .ifPresent(id -> cardsByVideo.computeIfAbsent(id, key -> new ArrayList<>()).add(item));
        }
        List<String> ids = new ArrayList<>(cardsByVideo.keySet());
        int saved = 0;
        for (int from = 0; from < ids.size(); from += BATCH_SIZE) {
            List<String> batch = ids.subList(from, Math.min(from + BATCH_SIZE, ids.size()));
            Map<String, VideoStats> stats;
            try {
                stats = youTubeClient.fetch(batch);
            } catch (RuntimeException e) {
                log.warn("YouTube refresh: batch of {} videos failed ({})", batch.size(), e.getClass().getSimpleName());
                continue;
            }
            for (String id : batch) {
                VideoStats videoStats = stats.get(id);
                if (videoStats == null) continue;
                for (KanbanItem item : cardsByVideo.get(id)) {
                    try {
                        videoStatsService.saveSnapshot(item, videoStats);
                        saved++;
                    } catch (RuntimeException e) {
                        log.warn("YouTube refresh: snapshot of card {} not saved ({})", item.getId(), e.getClass().getSimpleName());
                    }
                }
            }
        }
        return saved;
    }
}
