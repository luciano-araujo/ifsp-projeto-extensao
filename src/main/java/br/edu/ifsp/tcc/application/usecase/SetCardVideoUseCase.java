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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SetCardVideoUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;
    private final VideoStatsService videoStatsService;
    private final MaterialService materialService;
    private final CardReviewService cardReviewService;

    public SetCardVideoUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService,
                               VideoStatsService videoStatsService, MaterialService materialService,
                               CardReviewService cardReviewService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
        this.videoStatsService = videoStatsService;
        this.materialService = materialService;
        this.cardReviewService = cardReviewService;
    }

    /**
     * Links (or, with a blank url, unlinks) the YouTube video of a project. Choice: the snapshots of the card are
     * deleted when the link is cleared and when it changes to a different video; a new link to the same video
     * (another URL form) keeps the history.
     */
    @Transactional
    public CardResponse execute(Long teamId, Long userId, Long cardId, SetVideoDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        KanbanItem item = boardService.findCard(teamId, cardId);
        if (item.getState() == KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Converta a ideia em projeto antes de vincular um vídeo.");
        }
        String url = dto == null || dto.getUrl() == null ? "" : dto.getUrl().trim();
        String previousId = VideoStatsService.extractVideoId(item.getVideoUrl()).orElse(null);
        if (url.isEmpty()) {
            item.setVideoUrl(null);
            videoStatsService.deleteSnapshots(cardId);
        } else {
            String newId = VideoStatsService.extractVideoId(url)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, VideoStatsService.INVALID_URL));
            item.setVideoUrl(url);
            if (previousId != null && !newId.equals(previousId)) {
                videoStatsService.deleteSnapshots(cardId);
            }
        }
        KanbanItem saved = kanbanItemService.save(item);
        // A project without a column is shown in the first column of the board
        Long columnId = saved.getColumnId() != null ? saved.getColumnId()
                : boardService.findColumns(teamId).stream().map(BoardColumn::getId).findFirst().orElse(null);
        LocalDateTime columnSince = boardService.findColumnSince(List.of(saved)).get(saved.getId());
        ReviewStatus reviewStatus = cardReviewService.findStatuses(List.of(saved))
                .getOrDefault(saved.getId(), ReviewStatus.NONE);
        return boardService.toCardResponse(saved, columnId, boardService.findTasks(saved), columnSince,
                materialService.countByCard(saved.getId()), reviewStatus);
    }
}
