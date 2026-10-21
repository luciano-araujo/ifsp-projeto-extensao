package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateModuleDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.springframework.stereotype.Component;

@Component
public class CreateModuleUseCase {

    private final CurriculumService curriculumService;

    public CreateModuleUseCase(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    public Module execute(Long kanbanItemId, Long userId, CreateModuleDTO dto) {
        KanbanItem item = curriculumService.verifyOwnership(kanbanItemId, userId);

        Module module = new Module();
        module.setKanbanItem(item);
        module.setTitle(dto.getTitle());
        module.setSortOrder(curriculumService.countModulesByKanbanItemId(kanbanItemId));

        return curriculumService.saveModule(module);
    }
}
