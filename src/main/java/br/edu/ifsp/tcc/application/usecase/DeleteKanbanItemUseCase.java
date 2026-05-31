package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.springframework.stereotype.Component;

@Component
public class DeleteKanbanItemUseCase {

    private final KanbanItemService kanbanItemService;

    public DeleteKanbanItemUseCase(KanbanItemService kanbanItemService) {
        this.kanbanItemService = kanbanItemService;
    }

    public void execute(Long itemId, Long userId) {
        KanbanItem item = kanbanItemService.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));
        kanbanItemService.deleteById(item.getId());
    }
}
