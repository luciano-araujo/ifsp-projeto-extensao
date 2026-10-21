package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.CourseDTOs.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> list(@AuthenticationPrincipal User user, @PathVariable Long teamId) {
        return ResponseEntity.ok(courseService.listCourses(teamId, user.getId()));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> get(@AuthenticationPrincipal User user,
                                                    @PathVariable Long teamId,
                                                    @PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourse(teamId, user.getId(), courseId));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@AuthenticationPrincipal User user,
                                                 @PathVariable Long teamId,
                                                 @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(courseService.createCourse(teamId, user.getId(), request));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable Long teamId,
                                       @PathVariable Long courseId) {
        courseService.deleteCourse(teamId, user.getId(), courseId);
        return ResponseEntity.noContent().build();
    }

    // --- Modules ---

    @PostMapping("/{courseId}/modules")
    public ResponseEntity<CourseModuleResponse> createModule(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @Valid @RequestBody CreateCourseModuleRequest request) {
        return ResponseEntity.ok(courseService.createModule(teamId, user.getId(), courseId, request));
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}")
    public ResponseEntity<Void> deleteModule(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long courseId,
                                             @PathVariable Long moduleId) {
        courseService.deleteModule(teamId, user.getId(), courseId, moduleId);
        return ResponseEntity.noContent().build();
    }

    // --- Lessons ---

    @PostMapping("/{courseId}/modules/{moduleId}/lessons")
    public ResponseEntity<CourseLessonResponse> createLesson(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @PathVariable Long moduleId,
                                                             @Valid @RequestBody CreateCourseLessonRequest request) {
        return ResponseEntity.ok(courseService.createLesson(teamId, user.getId(), courseId, moduleId, request));
    }

    @PatchMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<CourseLessonResponse> updateLesson(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @PathVariable Long moduleId,
                                                             @PathVariable Long lessonId,
                                                             @RequestBody UpdateCourseLessonRequest request) {
        return ResponseEntity.ok(courseService.updateLesson(teamId, user.getId(), courseId, moduleId, lessonId, request));
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long courseId,
                                             @PathVariable Long moduleId,
                                             @PathVariable Long lessonId) {
        courseService.deleteLesson(teamId, user.getId(), courseId, moduleId, lessonId);
        return ResponseEntity.noContent().build();
    }
}
