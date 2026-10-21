package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateLessonDTO;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.springframework.stereotype.Component;

@Component
public class CreateLessonUseCase {

    private final CurriculumService curriculumService;

    public CreateLessonUseCase(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    public Lesson execute(Long kanbanItemId, Long userId, Long moduleId, CreateLessonDTO dto) {
        curriculumService.verifyOwnership(kanbanItemId, userId);
        Module module = curriculumService.findModule(kanbanItemId, moduleId);

        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle(dto.getTitle());
        lesson.setType(dto.getType());
        lesson.setSortOrder(curriculumService.countLessonsByModuleId(moduleId));

        return curriculumService.saveLesson(lesson);
    }
}
