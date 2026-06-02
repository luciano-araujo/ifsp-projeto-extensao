package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GetKanbanItemByIdUseCase {

    private final KanbanItemService kanbanItemService;

    public GetKanbanItemByIdUseCase(KanbanItemService kanbanItemService) {
        this.kanbanItemService = kanbanItemService;
    }

    public Optional<KanbanItem> execute(Long itemId, Long userId) {
        return kanbanItemService.findByIdAndUserId(itemId, userId);
    }
}
