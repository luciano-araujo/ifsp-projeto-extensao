package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.springframework.stereotype.Component;

@Component
public class UpdateKanbanItemUseCase {

    private final KanbanItemService kanbanItemService;

    public UpdateKanbanItemUseCase(KanbanItemService kanbanItemService) {
        this.kanbanItemService = kanbanItemService;
    }

    public KanbanItem execute(Long itemId, Long userId, UpdateKanbanItemDTO dto) {
        KanbanItem item = kanbanItemService.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));

        if (dto.getTitle() != null) item.setTitle(dto.getTitle());
        if (dto.getDescription() != null) item.setDescription(dto.getDescription());
        if (dto.getState() != null) item.setState(dto.getState());
        if (dto.getTargetAudience() != null) item.setTargetAudience(dto.getTargetAudience());
        if (dto.getPedagogicalObjective() != null) item.setPedagogicalObjective(dto.getPedagogicalObjective());
        if (dto.getProgress() != null) item.setProgress(dto.getProgress());

        return kanbanItemService.save(item);
    }
}
