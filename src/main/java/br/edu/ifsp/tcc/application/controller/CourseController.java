package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final UpdateCourseUseCase updateCourseUseCase;
    private final DeleteCourseUseCase deleteCourseUseCase;
    private final CreateCourseModuleUseCase createCourseModuleUseCase;
    private final UpdateCourseModuleUseCase updateCourseModuleUseCase;
    private final ReorderCourseModulesUseCase reorderCourseModulesUseCase;
    private final DeleteCourseModuleUseCase deleteCourseModuleUseCase;
    private final CreateCourseLessonUseCase createCourseLessonUseCase;
    private final UpdateCourseLessonUseCase updateCourseLessonUseCase;
    private final ReorderCourseLessonsUseCase reorderCourseLessonsUseCase;
    private final DeleteCourseLessonUseCase deleteCourseLessonUseCase;
    private final LinkLessonMaterialUseCase linkLessonMaterialUseCase;
    private final UnlinkLessonMaterialUseCase unlinkLessonMaterialUseCase;
    private final GetPublicationCheckUseCase getPublicationCheckUseCase;
    private final PublishCourseUseCase publishCourseUseCase;
    private final UnpublishCourseUseCase unpublishCourseUseCase;
    private final ExportCourseUseCase exportCourseUseCase;

    public CourseController(ListCoursesUseCase listCoursesUseCase,
                            GetCourseUseCase getCourseUseCase,
                            CreateCourseUseCase createCourseUseCase,
                            UpdateCourseUseCase updateCourseUseCase,
                            DeleteCourseUseCase deleteCourseUseCase,
                            CreateCourseModuleUseCase createCourseModuleUseCase,
                            UpdateCourseModuleUseCase updateCourseModuleUseCase,
                            ReorderCourseModulesUseCase reorderCourseModulesUseCase,
                            DeleteCourseModuleUseCase deleteCourseModuleUseCase,
                            CreateCourseLessonUseCase createCourseLessonUseCase,
                            UpdateCourseLessonUseCase updateCourseLessonUseCase,
                            ReorderCourseLessonsUseCase reorderCourseLessonsUseCase,
                            DeleteCourseLessonUseCase deleteCourseLessonUseCase,
                            LinkLessonMaterialUseCase linkLessonMaterialUseCase,
                            UnlinkLessonMaterialUseCase unlinkLessonMaterialUseCase,
                            GetPublicationCheckUseCase getPublicationCheckUseCase,
                            PublishCourseUseCase publishCourseUseCase,
                            UnpublishCourseUseCase unpublishCourseUseCase,
                            ExportCourseUseCase exportCourseUseCase) {
        this.listCoursesUseCase = listCoursesUseCase;
        this.getCourseUseCase = getCourseUseCase;
        this.createCourseUseCase = createCourseUseCase;
        this.updateCourseUseCase = updateCourseUseCase;
        this.deleteCourseUseCase = deleteCourseUseCase;
        this.createCourseModuleUseCase = createCourseModuleUseCase;
        this.updateCourseModuleUseCase = updateCourseModuleUseCase;
        this.reorderCourseModulesUseCase = reorderCourseModulesUseCase;
        this.deleteCourseModuleUseCase = deleteCourseModuleUseCase;
        this.createCourseLessonUseCase = createCourseLessonUseCase;
        this.updateCourseLessonUseCase = updateCourseLessonUseCase;
        this.reorderCourseLessonsUseCase = reorderCourseLessonsUseCase;
        this.deleteCourseLessonUseCase = deleteCourseLessonUseCase;
        this.linkLessonMaterialUseCase = linkLessonMaterialUseCase;
        this.unlinkLessonMaterialUseCase = unlinkLessonMaterialUseCase;
        this.getPublicationCheckUseCase = getPublicationCheckUseCase;
        this.publishCourseUseCase = publishCourseUseCase;
        this.unpublishCourseUseCase = unpublishCourseUseCase;
        this.exportCourseUseCase = exportCourseUseCase;
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

    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseResponse> update(@AuthenticationPrincipal User user,
                                                 @PathVariable Long teamId,
                                                 @PathVariable Long courseId,
                                                 @Valid @RequestBody UpdateCourseDTO dto) {
        return ResponseEntity.ok(updateCourseUseCase.execute(teamId, user.getId(), courseId, dto));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable Long teamId,
                                       @PathVariable Long courseId) {
        deleteCourseUseCase.execute(teamId, user.getId(), courseId);
        return ResponseEntity.noContent().build();
    }

    // --- Publication and export ---

    @GetMapping("/{courseId}/publication-check")
    public ResponseEntity<PublicationCheckResponse> publicationCheck(@AuthenticationPrincipal User user,
                                                                     @PathVariable Long teamId,
                                                                     @PathVariable Long courseId) {
        return ResponseEntity.ok(getPublicationCheckUseCase.execute(teamId, user.getId(), courseId));
    }

    @PostMapping("/{courseId}/publish")
    public ResponseEntity<CourseResponse> publish(@AuthenticationPrincipal User user,
                                                  @PathVariable Long teamId,
                                                  @PathVariable Long courseId) {
        return ResponseEntity.ok(publishCourseUseCase.execute(teamId, user.getId(), courseId));
    }

    @PostMapping("/{courseId}/unpublish")
    public ResponseEntity<CourseResponse> unpublish(@AuthenticationPrincipal User user,
                                                    @PathVariable Long teamId,
                                                    @PathVariable Long courseId) {
        return ResponseEntity.ok(unpublishCourseUseCase.execute(teamId, user.getId(), courseId));
    }

    @GetMapping("/{courseId}/export")
    public ResponseEntity<Object> export(@AuthenticationPrincipal User user,
                                         @PathVariable Long teamId,
                                         @PathVariable Long courseId,
                                         @RequestParam(defaultValue = "json") String format) {
        CourseExportFile file = exportCourseUseCase.execute(teamId, user.getId(), courseId, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"; filename*=UTF-8''" + file.getFileName())
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getBody());
    }

    // --- Modules ---

    @PostMapping("/{courseId}/modules")
    public ResponseEntity<CourseModuleResponse> createModule(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @Valid @RequestBody CreateModuleDTO dto) {
        return ResponseEntity.ok(createCourseModuleUseCase.execute(teamId, user.getId(), courseId, dto));
    }

    @PutMapping("/{courseId}/modules/order")
    public ResponseEntity<List<CourseModuleResponse>> reorderModules(@AuthenticationPrincipal User user,
                                                                     @PathVariable Long teamId,
                                                                     @PathVariable Long courseId,
                                                                     @Valid @RequestBody ReorderModulesDTO dto) {
        return ResponseEntity.ok(reorderCourseModulesUseCase.execute(teamId, user.getId(), courseId, dto));
    }

    @PatchMapping("/{courseId}/modules/{moduleId}")
    public ResponseEntity<CourseModuleResponse> updateModule(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @PathVariable Long moduleId,
                                                             @Valid @RequestBody UpdateModuleDTO dto) {
        return ResponseEntity.ok(updateCourseModuleUseCase.execute(teamId, user.getId(), courseId, moduleId, dto));
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

    @PutMapping("/{courseId}/modules/{moduleId}/lessons/order")
    public ResponseEntity<List<CourseLessonResponse>> reorderLessons(@AuthenticationPrincipal User user,
                                                                     @PathVariable Long teamId,
                                                                     @PathVariable Long courseId,
                                                                     @PathVariable Long moduleId,
                                                                     @Valid @RequestBody ReorderLessonsDTO dto) {
        return ResponseEntity.ok(reorderCourseLessonsUseCase.execute(teamId, user.getId(), courseId, moduleId, dto));
    }

    @PatchMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}")
    public ResponseEntity<CourseLessonResponse> updateLesson(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @PathVariable Long moduleId,
                                                             @PathVariable Long lessonId,
                                                             @Valid @RequestBody UpdateCourseLessonDTO dto) {
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

    // --- Lesson materials ---

    @PostMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}/materials")
    public ResponseEntity<CourseLessonResponse> linkMaterial(@AuthenticationPrincipal User user,
                                                             @PathVariable Long teamId,
                                                             @PathVariable Long courseId,
                                                             @PathVariable Long moduleId,
                                                             @PathVariable Long lessonId,
                                                             @Valid @RequestBody LinkLessonMaterialDTO dto) {
        return ResponseEntity.ok(linkLessonMaterialUseCase.execute(teamId, user.getId(), courseId, moduleId, lessonId, dto));
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}/lessons/{lessonId}/materials/{materialId}")
    public ResponseEntity<Void> unlinkMaterial(@AuthenticationPrincipal User user,
                                               @PathVariable Long teamId,
                                               @PathVariable Long courseId,
                                               @PathVariable Long moduleId,
                                               @PathVariable Long lessonId,
                                               @PathVariable Long materialId) {
        unlinkLessonMaterialUseCase.execute(teamId, user.getId(), courseId, moduleId, lessonId, materialId);
        return ResponseEntity.noContent().build();
    }
}
