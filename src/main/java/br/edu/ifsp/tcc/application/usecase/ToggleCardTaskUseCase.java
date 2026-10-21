package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ToggleCardTaskUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public ToggleCardTaskUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    /** Any member (reviewers included) can tick a task; the card progress is recomputed. */
    @Transactional
    public CardTaskResponse execute(Long teamId, Long userId, Long cardId, Long taskId) {
        teamService.requireMember(teamId, userId);
        KanbanItem item = boardService.findCard(teamId, cardId);
        CardTask task = boardService.findTask(cardId, taskId);

        task.setCompleted(!Boolean.TRUE.equals(task.getCompleted()));
        CardTask saved = boardService.saveTask(task);
        boardService.refreshProgress(item);
        return boardService.toTaskResponse(saved);
    }
}
