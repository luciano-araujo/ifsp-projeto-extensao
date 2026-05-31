package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetKanbanItemsByUserUseCase {

    private final KanbanItemService kanbanItemService;

    public GetKanbanItemsByUserUseCase(KanbanItemService kanbanItemService) {
        this.kanbanItemService = kanbanItemService;
    }

    public List<KanbanItem> execute(Long userId, KanbanItemState state) {
        if (state != null) {
            return kanbanItemService.findByUserIdAndState(userId, state);
        }
        return kanbanItemService.findByUserId(userId);
    }
}
