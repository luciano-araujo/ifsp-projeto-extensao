package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.VideoStatsResponse;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.VideoStatsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetCardVideoStatsUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final VideoStatsService videoStatsService;

    public GetCardVideoStatsUseCase(TeamService teamService, BoardService boardService, VideoStatsService videoStatsService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.videoStatsService = videoStatsService;
    }

    /** Latest statistics and history of the linked video. Any member can read them; 404 when the card has no link. */
    @Transactional(readOnly = true)
    public VideoStatsResponse execute(Long teamId, Long userId, Long cardId) {
        teamService.requireMember(teamId, userId);
        return videoStatsService.toResponse(boardService.findCard(teamId, cardId));
    }
}
