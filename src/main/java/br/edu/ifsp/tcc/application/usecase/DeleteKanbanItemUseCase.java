package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteKanbanItemUseCase {

    private final KanbanItemService kanbanItemService;
    private final TeamService teamService;
    private final MaterialService materialService;

    public DeleteKanbanItemUseCase(KanbanItemService kanbanItemService, TeamService teamService,
                                   MaterialService materialService) {
        this.kanbanItemService = kanbanItemService;
        this.teamService = teamService;
        this.materialService = materialService;
    }

    /** Editors (ADMIN and CREATOR of the card's team) only. */
    @Transactional
    public void execute(Long itemId, Long userId) {
        KanbanItem item = kanbanItemService.findCardOfMember(itemId, userId);
        teamService.requireRole(item.getTeamId(), userId, TeamRole.ADMIN, TeamRole.CREATOR);
        // The materials cascade away with the card; their stored files are removed after the commit
        materialService.scheduleFileRemovalOfCard(item.getId());
        kanbanItemService.deleteById(item.getId());
    }
}
