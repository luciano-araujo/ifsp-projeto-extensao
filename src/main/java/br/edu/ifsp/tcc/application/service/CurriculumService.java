package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CreateLessonDTO;
import br.edu.ifsp.tcc.application.dto.CreateModuleDTO;
import br.edu.ifsp.tcc.application.dto.UpdateLessonDTO;
import br.edu.ifsp.tcc.application.dto.UpdateModuleDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.repository.LessonRepository;
import br.edu.ifsp.tcc.application.repository.ModuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    // --- Ownership verification ---

    private KanbanItem verifyOwnership(Long kanbanItemId, Long userId) {
        return kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));
    }

    // --- Module operations ---

    public Module createModule(Long kanbanItemId, Long userId, CreateModuleDTO dto) {
        KanbanItem item = verifyOwnership(kanbanItemId, userId);

        Module module = new Module();
        module.setKanbanItem(item);
        module.setTitle(dto.getTitle());
        module.setSortOrder(moduleRepository.countByKanbanItem_Id(kanbanItemId));

        return moduleRepository.save(module);
    }

    public List<Module> listModules(Long kanbanItemId, Long userId) {
        verifyOwnership(kanbanItemId, userId);
        return moduleRepository.findByKanbanItemIdOrderBySortOrderAsc(kanbanItemId);
    }

    public Module updateModule(Long kanbanItemId, Long userId, Long moduleId, UpdateModuleDTO dto) {
        verifyOwnership(kanbanItemId, userId);

        Module module = moduleRepository.findByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        if (dto.getTitle() != null) module.setTitle(dto.getTitle());
        if (dto.getSortOrder() != null) module.setSortOrder(dto.getSortOrder());

        return moduleRepository.save(module);
    }

    public void deleteModule(Long kanbanItemId, Long userId, Long moduleId) {
        verifyOwnership(kanbanItemId, userId);

        moduleRepository.findByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        moduleRepository.deleteById(moduleId);
    }

    // --- Lesson operations ---

    public Lesson createLesson(Long kanbanItemId, Long userId, Long moduleId, CreateLessonDTO dto) {
        verifyOwnership(kanbanItemId, userId);

        Module module = moduleRepository.findByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle(dto.getTitle());
        lesson.setType(dto.getType());
        lesson.setSortOrder(lessonRepository.countByModule_Id(moduleId));

        return lessonRepository.save(lesson);
    }

    public Lesson updateLesson(Long kanbanItemId, Long userId, Long moduleId, Long lessonId, UpdateLessonDTO dto) {
        verifyOwnership(kanbanItemId, userId);

        moduleRepository.findByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        Lesson lesson = lessonRepository.findByIdAndModule_Id(lessonId, moduleId)
                .orElseThrow(() -> new RuntimeException("Aula nao encontrada."));

        if (dto.getTitle() != null) lesson.setTitle(dto.getTitle());
        if (dto.getType() != null) lesson.setType(dto.getType());
        if (dto.getPublished() != null) lesson.setPublished(dto.getPublished());
        if (dto.getSortOrder() != null) lesson.setSortOrder(dto.getSortOrder());

        return lessonRepository.save(lesson);
    }

    public void deleteLesson(Long kanbanItemId, Long userId, Long moduleId, Long lessonId) {
        verifyOwnership(kanbanItemId, userId);

        moduleRepository.findByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        lessonRepository.findByIdAndModule_Id(lessonId, moduleId)
                .orElseThrow(() -> new RuntimeException("Aula nao encontrada."));

        lessonRepository.deleteById(lessonId);
    }

    // --- Low-level accessors (kept for backward compatibility with tests) ---

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
