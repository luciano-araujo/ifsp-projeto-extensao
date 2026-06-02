package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.CurriculumService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kanban/{kanbanItemId}/curriculum")
public class CurriculumController {

    private final CurriculumService curriculumService;

    public CurriculumController(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    // --- Modules ---

    @PostMapping("/modules")
    public ResponseEntity<Module> createModule(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @Valid @RequestBody CreateModuleDTO dto) {
        Module module = curriculumService.createModule(kanbanItemId, user.getId(), dto);
        return ResponseEntity.ok(module);
    }

    @GetMapping("/modules")
    public ResponseEntity<List<Module>> listModules(@AuthenticationPrincipal User user,
                                                     @PathVariable Long kanbanItemId) {
        List<Module> modules = curriculumService.listModules(kanbanItemId, user.getId());
        return ResponseEntity.ok(modules);
    }

    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<Module> updateModule(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @RequestBody UpdateModuleDTO dto) {
        Module module = curriculumService.updateModule(kanbanItemId, user.getId(), moduleId, dto);
        return ResponseEntity.ok(module);
    }

    @DeleteMapping("/modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(@AuthenticationPrincipal User user,
                                              @PathVariable Long kanbanItemId,
                                              @PathVariable Long moduleId) {
        curriculumService.deleteModule(kanbanItemId, user.getId(), moduleId);
        return ResponseEntity.noContent().build();
    }

    // --- Lessons ---

    @PostMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<Lesson> createLesson(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @Valid @RequestBody CreateLessonDTO dto) {
        Lesson lesson = curriculumService.createLesson(kanbanItemId, user.getId(), moduleId, dto);
        return ResponseEntity.ok(lesson);
    }

    @PutMapping("/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Lesson> updateLesson(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @PathVariable Long lessonId,
                                                @RequestBody UpdateLessonDTO dto) {
        Lesson lesson = curriculumService.updateLesson(kanbanItemId, user.getId(), moduleId, lessonId, dto);
        return ResponseEntity.ok(lesson);
    }

    @DeleteMapping("/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@AuthenticationPrincipal User user,
                                              @PathVariable Long kanbanItemId,
                                              @PathVariable Long moduleId,
                                              @PathVariable Long lessonId) {
        curriculumService.deleteLesson(kanbanItemId, user.getId(), moduleId, lessonId);
        return ResponseEntity.noContent().build();
    }
}
