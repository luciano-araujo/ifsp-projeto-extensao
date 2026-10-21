package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.LessonType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

/** Request and response bodies of the team course routes (Curso -> Modulo -> Aula). */
public final class CourseDTOs {

    private CourseDTOs() {}

    private static final String ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public record CreateCourseRequest(
            @NotBlank(message = "O titulo do curso e obrigatorio.") String title,
            String description) {}

    public record CreateCourseModuleRequest(
            @NotBlank(message = "O titulo do modulo e obrigatorio.") String title) {}

    public record CreateCourseLessonRequest(
            @NotBlank(message = "O titulo da aula e obrigatorio.") String title,
            LessonType type,
            Long projectId) {}

    public record UpdateCourseLessonRequest(
            String title,
            LessonType type,
            Boolean published,
            Long projectId) {}

    public record CourseResponse(
            Long id,
            Long teamId,
            String title,
            String description,
            int modulesCount,
            @JsonFormat(pattern = ISO) LocalDateTime createdAt) {}

    public record CourseLessonResponse(
            Long id,
            Long moduleId,
            String title,
            LessonType type,
            boolean published,
            int sortOrder,
            Long projectId,
            @JsonFormat(pattern = ISO) LocalDateTime createdAt) {}

    public record CourseModuleResponse(
            Long id,
            Long courseId,
            String title,
            int sortOrder,
            List<CourseLessonResponse> lessons,
            @JsonFormat(pattern = ISO) LocalDateTime createdAt) {}

    public record CourseDetailResponse(
            Long id,
            Long teamId,
            String title,
            String description,
            int modulesCount,
            List<CourseModuleResponse> modules,
            @JsonFormat(pattern = ISO) LocalDateTime createdAt) {}
}
