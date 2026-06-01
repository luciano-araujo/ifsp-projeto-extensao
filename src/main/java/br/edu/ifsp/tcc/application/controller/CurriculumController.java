package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kanban/{kanbanItemId}/curriculum")
public class CurriculumController {

    private final CurriculumService curriculumService;
    private final KanbanItemService kanbanItemService;

    public CurriculumController(CurriculumService curriculumService, KanbanItemService kanbanItemService) {
        this.curriculumService = curriculumService;
        this.kanbanItemService = kanbanItemService;
    }

    private void verifyOwnership(Long kanbanItemId, Long userId) {
        kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));
    }

    // --- Modules ---

    @PostMapping("/modules")
    public ResponseEntity<Module> createModule(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @Valid @RequestBody CreateModuleDTO dto) {
        var item = kanbanItemService.findByIdAndUserId(kanbanItemId, user.getId())
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));

        Module module = new Module();
        module.setKanbanItem(item);
        module.setTitle(dto.getTitle());
        module.setSortOrder(curriculumService.countModulesByKanbanItemId(kanbanItemId));

        return ResponseEntity.ok(curriculumService.saveModule(module));
    }

    @GetMapping("/modules")
    public ResponseEntity<List<Module>> listModules(@AuthenticationPrincipal User user,
                                                     @PathVariable Long kanbanItemId) {
        verifyOwnership(kanbanItemId, user.getId());
        return ResponseEntity.ok(curriculumService.findModulesByKanbanItemId(kanbanItemId));
    }

    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<Module> updateModule(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @RequestBody UpdateModuleDTO dto) {
        verifyOwnership(kanbanItemId, user.getId());

        Module module = curriculumService.findModuleByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        if (dto.getTitle() != null) module.setTitle(dto.getTitle());
        if (dto.getSortOrder() != null) module.setSortOrder(dto.getSortOrder());

        return ResponseEntity.ok(curriculumService.saveModule(module));
    }

    @DeleteMapping("/modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(@AuthenticationPrincipal User user,
                                              @PathVariable Long kanbanItemId,
                                              @PathVariable Long moduleId) {
        verifyOwnership(kanbanItemId, user.getId());

        curriculumService.findModuleByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        curriculumService.deleteModule(moduleId);
        return ResponseEntity.noContent().build();
    }

    // --- Lessons ---

    @PostMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<Lesson> createLesson(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @Valid @RequestBody CreateLessonDTO dto) {
        verifyOwnership(kanbanItemId, user.getId());

        Module module = curriculumService.findModuleByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setTitle(dto.getTitle());
        lesson.setType(dto.getType());
        lesson.setSortOrder(curriculumService.countLessonsByModuleId(moduleId));

        return ResponseEntity.ok(curriculumService.saveLesson(lesson));
    }

    @PutMapping("/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Lesson> updateLesson(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @PathVariable Long lessonId,
                                                @RequestBody UpdateLessonDTO dto) {
        verifyOwnership(kanbanItemId, user.getId());

        curriculumService.findModuleByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        Lesson lesson = curriculumService.findLessonByIdAndModuleId(lessonId, moduleId)
                .orElseThrow(() -> new RuntimeException("Aula nao encontrada."));

        if (dto.getTitle() != null) lesson.setTitle(dto.getTitle());
        if (dto.getType() != null) lesson.setType(dto.getType());
        if (dto.getPublished() != null) lesson.setPublished(dto.getPublished());
        if (dto.getSortOrder() != null) lesson.setSortOrder(dto.getSortOrder());

        return ResponseEntity.ok(curriculumService.saveLesson(lesson));
    }

    @DeleteMapping("/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@AuthenticationPrincipal User user,
                                              @PathVariable Long kanbanItemId,
                                              @PathVariable Long moduleId,
                                              @PathVariable Long lessonId) {
        verifyOwnership(kanbanItemId, user.getId());

        curriculumService.findModuleByIdAndKanbanItemId(moduleId, kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Modulo nao encontrado."));

        curriculumService.findLessonByIdAndModuleId(lessonId, moduleId)
                .orElseThrow(() -> new RuntimeException("Aula nao encontrada."));

        curriculumService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }
}
