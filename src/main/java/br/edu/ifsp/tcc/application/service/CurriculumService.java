package br.edu.ifsp.tcc.application.service;

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

    public CurriculumService(ModuleRepository moduleRepository, LessonRepository lessonRepository) {
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
    }

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
