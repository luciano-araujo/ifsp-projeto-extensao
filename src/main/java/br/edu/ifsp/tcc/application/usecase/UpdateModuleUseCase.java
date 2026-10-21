package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateModuleDTO;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.springframework.stereotype.Component;

@Component
public class UpdateModuleUseCase {

    private final CurriculumService curriculumService;

    public UpdateModuleUseCase(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    public Module execute(Long kanbanItemId, Long userId, Long moduleId, UpdateModuleDTO dto) {
        curriculumService.verifyOwnership(kanbanItemId, userId);
        Module module = curriculumService.findModule(kanbanItemId, moduleId);

        if (dto.getTitle() != null) module.setTitle(dto.getTitle());
        if (dto.getSortOrder() != null) module.setSortOrder(dto.getSortOrder());

        return curriculumService.saveModule(module);
    }
}
