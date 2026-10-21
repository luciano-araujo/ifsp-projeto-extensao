package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class DeleteColumnUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;

    public DeleteColumnUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
    }

    /** Deletes a column; its cards go to targetColumnId, or to the first remaining column. */
    @Transactional
    public void execute(Long teamId, Long userId, Long columnId, Long targetColumnId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        BoardColumn column = boardService.findColumn(teamId, columnId);

        List<BoardColumn> others = boardService.findColumns(teamId).stream()
                .filter(c -> !c.getId().equals(columnId))
                .toList();
        if (others.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O quadro precisa de pelo menos uma coluna.");
        }
        if (columnId.equals(targetColumnId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha outra coluna para receber os cards.");
        }
        BoardColumn target = targetColumnId != null ? boardService.findColumn(teamId, targetColumnId) : others.get(0);

        for (KanbanItem item : kanbanItemService.findByTeamId(teamId)) {
            if (columnId.equals(item.getColumnId())) {
                item.setColumn(target);
                kanbanItemService.save(item);
            }
        }
        boardService.deleteColumn(column);
    }
}
