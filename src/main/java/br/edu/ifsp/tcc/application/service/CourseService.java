package br.edu.ifsp.tcc.application.service;

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
                countModules(course.getId()), course.getCreatedAt());
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
        return new CourseLessonResponse(lesson.getId(), lesson.getUuid(), lesson.getModuleId(), lesson.getTitle(), lesson.getType(),
                Boolean.TRUE.equals(lesson.getPublished()), lesson.getSortOrder() != null ? lesson.getSortOrder() : 0,
                lesson.getProjectId(), lesson.getCreatedAt(), materials);
    }

    /** A lesson with its linked materials loaded from the database. */
    public CourseLessonResponse toLessonResponseWithMaterials(Long teamId, CourseLesson lesson) {
        return toLessonResponse(lesson, materialService.findResponsesByLesson(teamId, List.of(lesson.getId()))
                .getOrDefault(lesson.getId(), List.of()));
    }
}
