package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.VideoStatsResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.VideoStats;
import br.edu.ifsp.tcc.application.service.VideoStatsService;
import br.edu.ifsp.tcc.application.service.YouTubeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class RefreshCardVideoStatsUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final VideoStatsService videoStatsService;
    private final YouTubeClient youTubeClient;

    @Value("${app.youtube.cache-minutes:15}")
    private long cacheMinutes = 15;

    public RefreshCardVideoStatsUseCase(TeamService teamService, BoardService boardService,
                                        VideoStatsService videoStatsService, YouTubeClient youTubeClient) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.videoStatsService = videoStatsService;
        this.youTubeClient = youTubeClient;
    }

    /**
     * Not transactional on purpose: the YouTube call must not hold a database connection.
     * A snapshot younger than the cache window is returned without calling YouTube.
     */
    public VideoStatsResponse execute(Long teamId, Long userId, Long cardId) {
        teamService.requireMember(teamId, userId);
        KanbanItem item = boardService.findCard(teamId, cardId);
        String videoId = videoStatsService.requireVideoId(item);
        boolean cached = videoStatsService.findLatest(cardId)
                .filter(latest -> VideoStatsService.isFresh(latest, cacheMinutes)).isPresent();
        if (!cached) {
            if (!youTubeClient.isConfigured()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Integração com o YouTube não configurada.");
            }
            VideoStats stats = youTubeClient.fetch(List.of(videoId)).get(videoId);
            if (stats == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vídeo não encontrado ou privado no YouTube.");
            }
            videoStatsService.saveSnapshot(item, stats);
        }
        return videoStatsService.toResponse(item);
    }
}
