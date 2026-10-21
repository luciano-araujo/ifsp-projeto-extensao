package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.CourseResponse;
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

    public CourseService(CourseRepository courseRepository,
                         CourseModuleRepository moduleRepository,
                         CourseLessonRepository lessonRepository,
                         KanbanItemService kanbanItemService) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.kanbanItemService = kanbanItemService;
    }

    // --- Courses ---

    public List<Course> findCourses(Long teamId) {
        return courseRepository.findByTeam_IdOrderByCreatedAtDesc(teamId);
    }

    public Course findCourse(Long teamId, Long courseId) {
        return courseRepository.findByIdAndTeam_Id(courseId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso nao encontrado."));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo nao encontrado."));
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

    public CourseLesson findLesson(Long moduleId, Long lessonId) {
        return lessonRepository.findByIdAndModule_Id(lessonId, moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula nao encontrada."));
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
                        "O projeto vinculado nao pertence a esta equipe."));
    }

    // --- Response mapping ---

    public CourseResponse toCourseResponse(Course course) {
        return new CourseResponse(course.getId(), course.getTeamId(), course.getTitle(), course.getDescription(),
                countModules(course.getId()), course.getCreatedAt());
    }

    public CourseModuleResponse toModuleResponse(CourseModule module, List<CourseLesson> lessons) {
        return new CourseModuleResponse(module.getId(), module.getCourseId(), module.getTitle(), module.getSortOrder(),
                lessons.stream().map(this::toLessonResponse).toList(), module.getCreatedAt());
    }

    public CourseLessonResponse toLessonResponse(CourseLesson lesson) {
        return new CourseLessonResponse(lesson.getId(), lesson.getModuleId(), lesson.getTitle(), lesson.getType(),
                Boolean.TRUE.equals(lesson.getPublished()), lesson.getSortOrder() != null ? lesson.getSortOrder() : 0,
                lesson.getProjectId(), lesson.getCreatedAt());
    }
}
