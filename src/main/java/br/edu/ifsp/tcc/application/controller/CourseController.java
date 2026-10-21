package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/courses")
public class CourseController {

    private final ListCoursesUseCase listCoursesUseCase;
    private final GetCourseUseCase getCourseUseCase;
    private final CreateCourseUseCase createCourseUseCase;
    private final DeleteCourseUseCase deleteCourseUseCase;
    private final CreateCourseModuleUseCase createCourseModuleUseCase;
    private final DeleteCourseModuleUseCase deleteCourseModuleUseCase;
    private final CreateCourseLessonUseCase createCourseLessonUseCase;
    private final UpdateCourseLessonUseCase updateCourseLessonUseCase;
    private final DeleteCourseLessonUseCase deleteCourseLessonUseCase;

    public CourseController(ListCoursesUseCase listCoursesUseCase,
                            GetCourseUseCase getCourseUseCase,
                            CreateCourseUseCase createCourseUseCase,
                            DeleteCourseUseCase deleteCourseUseCase,
                            CreateCourseModuleUseCase createCourseModuleUseCase,
                            DeleteCourseModuleUseCase deleteCourseModuleUseCase,
                            CreateCourseLessonUseCase createCourseLessonUseCase,
                            UpdateCourseLessonUseCase updateCourseLessonUseCase,
                            DeleteCourseLessonUseCase deleteCourseLessonUseCase) {
        this.listCoursesUseCase = listCoursesUseCase;
        this.getCourseUseCase = getCourseUseCase;
        this.createCourseUseCase = createCourseUseCase;
        this.deleteCourseUseCase = deleteCourseUseCase;
        this.createCourseModuleUseCase = createCourseModuleUseCase;
        this.deleteCourseModuleUseCase = deleteCourseModuleUseCase;
        this.createCourseLessonUseCase = createCourseLessonUseCase;
        this.updateCourseLessonUseCase = updateCourseLessonUseCase;
        this.deleteCourseLessonUseCase = deleteCourseLessonUseCase;
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> list(@AuthenticationPrincipal User user, @PathVariable Long teamId) {
        return ResponseEntity.ok(listCoursesUseCase.execute(teamId, user.getId()));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> get(@AuthenticationPrincipal User user,
                                                    @PathVariable Long teamId,
                                                    @PathVariable Long courseId) {
        return ResponseEntity.ok(getCourseUseCase.execute(teamId, user.getId(), courseId));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@AuthenticationPrincipal User user,
                                                 @PathVariable Long teamId,
                                                 @Valid @RequestBody CreateCourseDTO dto) {
        return ResponseEntity.ok(createCourseUseCase.execute(teamId, user.getId(), dto));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable Long teamId,
                                       @PathVariable Long courseId) {
        deleteCourseUseCase.execute(teamId, user.getId(), courseId);
        return ResponseEntity.noContent().build();
    }

    // --- Modules ---

    @PostMapping("/{courseId}/modules")
    public ResponseEntity<CourseModuleResponse> createModule(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @Valid @RequestBody CreateModuleDTO dto) {
        return ResponseEntity.ok(createCourseModuleUseCase.execute(teamId, user.getId(), courseId, dto));
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long courseId,
                                             @PathVariable Long moduleId) {
        deleteCourseModuleUseCase.execute(teamId, user.getId(), courseId, moduleId);
        return ResponseEntity.noContent().build();
    }

    // --- Lessons ---

    @PostMapping("/{courseId}/modules/{moduleId}/lessons")
    public ResponseEntity<CourseLessonResponse> createLesson(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @PathVariable Long moduleId,
                                                             @Valid @RequestBody CreateCourseLessonDTO dto) {
        return ResponseEntity.ok(createCourseLessonUseCase.execute(teamId, user.getId(), courseId, moduleId, dto));
    }

    @PatchMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<CourseLessonResponse> updateLesson(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @PathVariable Long moduleId,
                                                             @PathVariable Long lessonId,
                                                             @RequestBody UpdateCourseLessonDTO dto) {
        return ResponseEntity.ok(updateCourseLessonUseCase.execute(teamId, user.getId(), courseId, moduleId, lessonId, dto));
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long courseId,
                                             @PathVariable Long moduleId,
                                             @PathVariable Long lessonId) {
        deleteCourseLessonUseCase.execute(teamId, user.getId(), courseId, moduleId, lessonId);
        return ResponseEntity.noContent().build();
    }
}
