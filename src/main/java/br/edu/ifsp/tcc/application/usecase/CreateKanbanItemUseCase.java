package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import org.springframework.stereotype.Component;

@Component
public class CreateKanbanItemUseCase {

    private final KanbanItemService kanbanItemService;

    public CreateKanbanItemUseCase(KanbanItemService kanbanItemService) {
        this.kanbanItemService = kanbanItemService;
    }

    public KanbanItem execute(CreateKanbanItemDTO dto, User user) {
        KanbanItem item = new KanbanItem();
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setState(KanbanItemState.IDEATION);
        item.setUser(user);
        return kanbanItemService.save(item);
    }
}
