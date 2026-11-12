package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.springframework.stereotype.Component;

@Component
public class GetKanbanItemByIdUseCase {

    private final KanbanItemService kanbanItemService;

    public GetKanbanItemByIdUseCase(KanbanItemService kanbanItemService) {
        this.kanbanItemService = kanbanItemService;
    }

    /** Any member of the card's team can read it. */
    public KanbanItem execute(Long itemId, Long userId) {
        return kanbanItemService.findCardOfMember(itemId, userId);
    }
}
