package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ConvertToProjectDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.springframework.stereotype.Component;

@Component
public class ConvertToProjectUseCase {

    private final KanbanItemService kanbanItemService;

    public ConvertToProjectUseCase(KanbanItemService kanbanItemService) {
        this.kanbanItemService = kanbanItemService;
    }

    public KanbanItem execute(Long itemId, Long userId, ConvertToProjectDTO dto) {
        KanbanItem item = kanbanItemService.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));

        if (item.getState() != KanbanItemState.IDEATION) {
            throw new RuntimeException("Somente itens em fase de ideacao podem ser convertidos em projeto.");
        }

        item.setTitle(dto.getTitle());
        item.setTargetAudience(dto.getTargetAudience());
        item.setPedagogicalObjective(dto.getPedagogicalObjective());
        item.setState(KanbanItemState.IN_PRODUCTION);

        return kanbanItemService.save(item);
    }
}
