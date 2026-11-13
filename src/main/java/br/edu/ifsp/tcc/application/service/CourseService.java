package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.CourseLessonRepository;
import br.edu.ifsp.tcc.application.repository.CourseModuleRepository;
import br.edu.ifsp.tcc.application.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data access and shared lookups of team courses (Curso -> Modulo -> Aula).
 * Business rules live in the course use cases.
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final CourseLessonRepository lessonRepository;
    private final KanbanItemService kanbanItemService;
    private final MaterialService materialService;

    public CourseService(CourseRepository courseRepository,
                         CourseModuleRepository moduleRepository,
                         CourseLessonRepository lessonRepository,
                         KanbanItemService kanbanItemService,
                         MaterialService materialService) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.kanbanItemService = kanbanItemService;
        this.materialService = materialService;
    }

    // --- Courses ---

    public List<Course> findCourses(Long teamId) {
        return courseRepository.findByTeam_IdOrderByCreatedAtDesc(teamId);
    }

    public Course findCourse(Long teamId, Long courseId) {
        return courseRepository.findByIdAndTeam_Id(courseId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso não encontrado."));
    }

    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    public void deleteCourse(Course course) {
        courseRepository.delete(course);
    }

    // --- Modules ---

    public List<CourseModule> findModules(Long courseId) {
        return moduleRepository.findByCourse_IdOrderBySortOrderAscIdAsc(courseId);
    }

    public CourseModule findModule(Long courseId, Long moduleId) {
        return moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo não encontrado."));
    }

    public int countModules(Long courseId) {
        return moduleRepository.countByCourse_Id(courseId);
    }

    public CourseModule saveModule(CourseModule module) {
        return moduleRepository.save(module);
    }

    public void deleteModule(CourseModule module) {
        moduleRepository.delete(module);
    }

    // --- Lessons ---

    public Map<Long, List<CourseLesson>> findLessonsByModule(List<CourseModule> modules) {
        if (modules.isEmpty()) return Map.of();
        return lessonRepository.findByModule_IdInOrderBySortOrderAscIdAsc(modules.stream().map(CourseModule::getId).toList())
                .stream().collect(Collectors.groupingBy(CourseLesson::getModuleId));
    }

    /** Materials of all the lessons of a course (one query), by lesson id; lessons without materials are absent. */
    public Map<Long, List<MaterialResponse>> findMaterialsByLesson(Long teamId, Map<Long, List<CourseLesson>> lessonsByModule) {
        List<Long> lessonIds = lessonsByModule.values().stream().flatMap(List::stream).map(CourseLesson::getId).toList();
        return materialService.findResponsesByLesson(teamId, lessonIds);
    }

    public CourseLesson findLesson(Long moduleId, Long lessonId) {
        return lessonRepository.findByIdAndModule_Id(lessonId, moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada."));
    }

    public int countLessons(Long moduleId) {
        return lessonRepository.countByModule_Id(moduleId);
    }

    public CourseLesson saveLesson(CourseLesson lesson) {
        return lessonRepository.save(lesson);
    }

    public void deleteLesson(CourseLesson lesson) {
        lessonRepository.delete(lesson);
    }

    /** A lesson can only link to a card of the same team. */
    public KanbanItem findProject(Long teamId, Long projectId) {
        if (projectId == null) return null;
        return kanbanItemService.findByIdAndTeamId(projectId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O projeto vinculado não pertence a esta equipe."));
    }

    // --- Response mapping ---

    public CourseResponse toCourseResponse(Course course) {
        return new CourseResponse(course.getId(), course.getUuid(), course.getTeamId(), course.getTitle(), course.getDescription(),
                course.getStatus(), course.getPublishedAt(), countModules(course.getId()), course.getCreatedAt());
    }

    /** The whole course tree (modules and lessons in order, with their materials). */
    public CourseDetailResponse toDetailResponse(Long teamId, Course course) {
        List<CourseModule> modules = findModules(course.getId());
        return new CourseDetailResponse(course.getId(), course.getUuid(), teamId, course.getTitle(), course.getDescription(),
                course.getStatus(), course.getPublishedAt(), modules.size(), toModuleResponses(teamId, modules), course.getCreatedAt());
    }

    /** Modules with their lessons and materials (a few queries for all of them). */
    public List<CourseModuleResponse> toModuleResponses(Long teamId, List<CourseModule> modules) {
        Map<Long, List<CourseLesson>> lessonsByModule = findLessonsByModule(modules);
        Map<Long, List<MaterialResponse>> materialsByLesson = findMaterialsByLesson(teamId, lessonsByModule);
        return modules.stream()
                .map(module -> toModuleResponse(module, lessonsByModule.getOrDefault(module.getId(), List.of()), materialsByLesson))
                .toList();
    }

    /** The lessons of a module, in order. */
    public List<CourseLesson> findLessons(Long moduleId) {
        return lessonRepository.findByModule_IdInOrderBySortOrderAscIdAsc(List.of(moduleId));
    }

    /** Trimmed text, or null when blank (an empty text clears an optional field). */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** A reorder request must list each existing id exactly once (no missing, extra or repeated ids). */
    public void requireExactIds(List<Long> requested, List<Long> existing, String message) {
        if (requested.size() != existing.size() || !Set.copyOf(requested).equals(Set.copyOf(existing))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    public CourseModuleResponse toModuleResponse(CourseModule module, List<CourseLesson> lessons) {
        return toModuleResponse(module, lessons, Map.of());
    }

    public CourseModuleResponse toModuleResponse(CourseModule module, List<CourseLesson> lessons,
                                                 Map<Long, List<MaterialResponse>> materialsByLesson) {
        return new CourseModuleResponse(module.getId(), module.getUuid(), module.getCourseId(), module.getTitle(), module.getSortOrder(),
                lessons.stream().map(l -> toLessonResponse(l, materialsByLesson.getOrDefault(l.getId(), List.of()))).toList(),
                module.getCreatedAt());
    }

    /** A lesson without materials (for example one that was just created). */
    public CourseLessonResponse toLessonResponse(CourseLesson lesson) {
        return toLessonResponse(lesson, List.of());
    }

    public CourseLessonResponse toLessonResponse(CourseLesson lesson, List<MaterialResponse> materials) {
        return new CourseLessonResponse(lesson.getId(), lesson.getUuid(), lesson.getModuleId(), lesson.getTitle(), lesson.getDescription(), lesson.getContentUrl(), lesson.getType(),
                Boolean.TRUE.equals(lesson.getPublished()), lesson.getSortOrder() != null ? lesson.getSortOrder() : 0,
                lesson.getProjectId(), lesson.getCreatedAt(), materials);
    }

    /** A lesson with its linked materials loaded from the database. */
    public CourseLessonResponse toLessonResponseWithMaterials(Long teamId, CourseLesson lesson) {
        return toLessonResponse(lesson, materialService.findResponsesByLesson(teamId, List.of(lesson.getId()))
                .getOrDefault(lesson.getId(), List.of()));
    }
}
