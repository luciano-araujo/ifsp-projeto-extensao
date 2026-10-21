package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.repository.LessonRepository;
import br.edu.ifsp.tcc.application.repository.ModuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Data access and lookups of the project curriculum (modules and lessons of a kanban item).
 * Business rules live in the curriculum use cases.
 */
@Service
public class CurriculumService {

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final KanbanItemService kanbanItemService;

    public CurriculumService(ModuleRepository moduleRepository,
                             LessonRepository lessonRepository,
                             KanbanItemService kanbanItemService) {
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.kanbanItemService = kanbanItemService;
    }

    // --- Lookups ---

    /** The kanban item, when the user owns it or belongs to its team. */
    public KanbanItem verifyOwnership(Long kanbanItemId, Long userId) {
        return kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));
    }

    public Module findModule(Long kanbanItemId, Long moduleId) {
        return moduleRepository.findByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));
    }

    public Lesson findLesson(Long moduleId, Long lessonId) {
        return lessonRepository.findByIdAndModule_Id(lessonId, moduleId)
                .orElseThrow(() -> new RuntimeException("Aula nao encontrada."));
    }

    // --- Modules ---

    public Module saveModule(Module module) {
        return moduleRepository.save(module);
    }

    public List<Module> findModulesByKanbanItemId(Long kanbanItemId) {
        return moduleRepository.findByKanbanItemIdOrderBySortOrderAsc(kanbanItemId);
    }

    public Optional<Module> findModuleByIdAndKanbanItemId(Long moduleId, Long kanbanItemId) {
        return moduleRepository.findByIdAndKanbanItemId(moduleId, kanbanItemId);
    }

    public int countModulesByKanbanItemId(Long kanbanItemId) {
        return moduleRepository.countByKanbanItem_Id(kanbanItemId);
    }

    public void deleteModule(Long moduleId) {
        moduleRepository.deleteById(moduleId);
    }

    // --- Lessons ---

    public Lesson saveLesson(Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    public Optional<Lesson> findLessonByIdAndModuleId(Long lessonId, Long moduleId) {
        return lessonRepository.findByIdAndModule_Id(lessonId, moduleId);
    }

    public int countLessonsByModuleId(Long moduleId) {
        return lessonRepository.countByModule_Id(moduleId);
    }

    public void deleteLesson(Long lessonId) {
        lessonRepository.deleteById(lessonId);
    }
}
