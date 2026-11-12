package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteKanbanItemUseCase {

    private final KanbanItemService kanbanItemService;
    private final TeamService teamService;

    public DeleteKanbanItemUseCase(KanbanItemService kanbanItemService, TeamService teamService) {
        this.kanbanItemService = kanbanItemService;
        this.teamService = teamService;
    }

    /** Editors (ADMIN and CREATOR of the card's team) only. */
    @Transactional
    public void execute(Long itemId, Long userId) {
        KanbanItem item = kanbanItemService.findCardOfMember(itemId, userId);
        teamService.requireRole(item.getTeamId(), userId, TeamRole.ADMIN, TeamRole.CREATOR);
        kanbanItemService.deleteById(item.getId());
    }
}
