package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.Lesson;
import br.edu.ifsp.tcc.application.entity.Module;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kanban/{kanbanItemId}/curriculum")
public class CurriculumController {

    private final ListModulesUseCase listModulesUseCase;
    private final CreateModuleUseCase createModuleUseCase;
    private final UpdateModuleUseCase updateModuleUseCase;
    private final DeleteModuleUseCase deleteModuleUseCase;
    private final CreateLessonUseCase createLessonUseCase;
    private final UpdateLessonUseCase updateLessonUseCase;
    private final DeleteLessonUseCase deleteLessonUseCase;

    public CurriculumController(ListModulesUseCase listModulesUseCase,
                                CreateModuleUseCase createModuleUseCase,
                                UpdateModuleUseCase updateModuleUseCase,
                                DeleteModuleUseCase deleteModuleUseCase,
                                CreateLessonUseCase createLessonUseCase,
                                UpdateLessonUseCase updateLessonUseCase,
                                DeleteLessonUseCase deleteLessonUseCase) {
        this.listModulesUseCase = listModulesUseCase;
        this.createModuleUseCase = createModuleUseCase;
        this.updateModuleUseCase = updateModuleUseCase;
        this.deleteModuleUseCase = deleteModuleUseCase;
        this.createLessonUseCase = createLessonUseCase;
        this.updateLessonUseCase = updateLessonUseCase;
        this.deleteLessonUseCase = deleteLessonUseCase;
    }

    // --- Modules ---

    @PostMapping("/modules")
    public ResponseEntity<Module> createModule(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @Valid @RequestBody CreateModuleDTO dto) {
        Module module = createModuleUseCase.execute(kanbanItemId, user.getId(), dto);
        return ResponseEntity.ok(module);
    }

    @GetMapping("/modules")
    public ResponseEntity<List<Module>> listModules(@AuthenticationPrincipal User user,
                                                     @PathVariable Long kanbanItemId) {
        List<Module> modules = listModulesUseCase.execute(kanbanItemId, user.getId());
        return ResponseEntity.ok(modules);
    }

    @PutMapping("/modules/{moduleId}")
    public ResponseEntity<Module> updateModule(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @RequestBody UpdateModuleDTO dto) {
        Module module = updateModuleUseCase.execute(kanbanItemId, user.getId(), moduleId, dto);
        return ResponseEntity.ok(module);
    }

    @DeleteMapping("/modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(@AuthenticationPrincipal User user,
                                              @PathVariable Long kanbanItemId,
                                              @PathVariable Long moduleId) {
        deleteModuleUseCase.execute(kanbanItemId, user.getId(), moduleId);
        return ResponseEntity.noContent().build();
    }

    // --- Lessons ---

    @PostMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<Lesson> createLesson(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @Valid @RequestBody CreateLessonDTO dto) {
        Lesson lesson = createLessonUseCase.execute(kanbanItemId, user.getId(), moduleId, dto);
        return ResponseEntity.ok(lesson);
    }

    @PutMapping("/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Lesson> updateLesson(@AuthenticationPrincipal User user,
                                                @PathVariable Long kanbanItemId,
                                                @PathVariable Long moduleId,
                                                @PathVariable Long lessonId,
                                                @RequestBody UpdateLessonDTO dto) {
        Lesson lesson = updateLessonUseCase.execute(kanbanItemId, user.getId(), moduleId, lessonId, dto);
        return ResponseEntity.ok(lesson);
    }

    @DeleteMapping("/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@AuthenticationPrincipal User user,
                                              @PathVariable Long kanbanItemId,
                                              @PathVariable Long moduleId,
                                              @PathVariable Long lessonId) {
        deleteLessonUseCase.execute(kanbanItemId, user.getId(), moduleId, lessonId);
        return ResponseEntity.noContent().build();
    }
}
