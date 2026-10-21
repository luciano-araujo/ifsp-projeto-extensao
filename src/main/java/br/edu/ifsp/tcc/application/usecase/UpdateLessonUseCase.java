package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateLessonDTO;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import org.springframework.stereotype.Component;

@Component
public class UpdateLessonUseCase {

    private final CurriculumService curriculumService;

    public UpdateLessonUseCase(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    public Lesson execute(Long kanbanItemId, Long userId, Long moduleId, Long lessonId, UpdateLessonDTO dto) {
        curriculumService.verifyOwnership(kanbanItemId, userId);
        curriculumService.findModule(kanbanItemId, moduleId);
        Lesson lesson = curriculumService.findLesson(moduleId, lessonId);

        if (dto.getTitle() != null) lesson.setTitle(dto.getTitle());
        if (dto.getType() != null) lesson.setType(dto.getType());
        if (dto.getPublished() != null) lesson.setPublished(dto.getPublished());
        if (dto.getSortOrder() != null) lesson.setSortOrder(dto.getSortOrder());

        return curriculumService.saveLesson(lesson);
    }
}
