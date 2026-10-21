package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.springframework.stereotype.Component;

@Component
public class DeleteLessonUseCase {

    private final CurriculumService curriculumService;

    public DeleteLessonUseCase(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    public void execute(Long kanbanItemId, Long userId, Long moduleId, Long lessonId) {
        curriculumService.verifyOwnership(kanbanItemId, userId);
        curriculumService.findModule(kanbanItemId, moduleId);
        curriculumService.findLesson(moduleId, lessonId);
        curriculumService.deleteLesson(lessonId);
    }
}
