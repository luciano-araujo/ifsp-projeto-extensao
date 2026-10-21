package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.springframework.stereotype.Component;

@Component
public class DeleteModuleUseCase {

    private final CurriculumService curriculumService;

    public DeleteModuleUseCase(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    public void execute(Long kanbanItemId, Long userId, Long moduleId) {
        curriculumService.verifyOwnership(kanbanItemId, userId);
        curriculumService.findModule(kanbanItemId, moduleId);
        curriculumService.deleteModule(moduleId);
    }
}
