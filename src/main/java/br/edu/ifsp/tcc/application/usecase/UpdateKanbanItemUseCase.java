package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class UpdateKanbanItemUseCase {

    private final KanbanItemService kanbanItemService;
    private final TeamService teamService;

    public UpdateKanbanItemUseCase(KanbanItemService kanbanItemService, TeamService teamService) {
        this.kanbanItemService = kanbanItemService;
        this.teamService = teamService;
    }

    /** Editors (ADMIN and CREATOR of the card's team) only; position and progress come from the board. */
    public KanbanItem execute(Long itemId, Long userId, UpdateKanbanItemDTO dto) {
        KanbanItem item = kanbanItemService.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));
        teamService.requireRole(item.getTeamId(), userId, TeamRole.ADMIN, TeamRole.CREATOR);

        if (dto.getTitle() != null) item.setTitle(dto.getTitle());
        if (dto.getDescription() != null) item.setDescription(dto.getDescription());
        if (dto.getTargetAudience() != null) item.setTargetAudience(dto.getTargetAudience());
        if (dto.getPedagogicalObjective() != null) item.setPedagogicalObjective(dto.getPedagogicalObjective());
        if (dto.getDueDate() != null) {
            item.setDueDate(dto.getDueDate().isEmpty() ? null : LocalDate.parse(dto.getDueDate()));
        }

        return kanbanItemService.save(item);
    }
}
