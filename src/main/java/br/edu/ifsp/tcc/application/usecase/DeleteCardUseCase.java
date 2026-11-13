package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteCardUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;
    private final MaterialService materialService;

    public DeleteCardUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService,
                             MaterialService materialService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
        this.materialService = materialService;
    }

    @Transactional
    public void execute(Long teamId, Long userId, Long cardId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        KanbanItem item = boardService.findCard(teamId, cardId);
        // The materials cascade away with the card; their stored files are removed after the commit
        materialService.scheduleFileRemovalOfCard(item.getId());
        kanbanItemService.deleteById(item.getId());
    }
}
