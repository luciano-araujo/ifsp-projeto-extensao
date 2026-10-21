package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.dto.CreateTaskDTO;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateCardTaskUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public CreateCardTaskUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    @Transactional
    public CardTaskResponse execute(Long teamId, Long userId, Long cardId, CreateTaskDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        KanbanItem item = boardService.findCard(teamId, cardId);

        CardTask task = new CardTask();
        task.setKanbanItem(item);
        task.setTitle(dto.getTitle().trim());
        task.setSortOrder(boardService.countTasks(cardId));
        CardTask saved = boardService.saveTask(task);
        boardService.refreshProgress(item);
        return boardService.toTaskResponse(saved);
    }
}
