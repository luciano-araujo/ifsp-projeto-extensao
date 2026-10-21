package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListModulesUseCase {

    private final CurriculumService curriculumService;

    public ListModulesUseCase(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    public List<Module> execute(Long kanbanItemId, Long userId) {
        curriculumService.verifyOwnership(kanbanItemId, userId);
        return curriculumService.findModulesByKanbanItemId(kanbanItemId);
    }
}
