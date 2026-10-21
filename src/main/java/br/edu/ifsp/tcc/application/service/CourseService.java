package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CourseDTOs.*;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.CourseLessonRepository;
import br.edu.ifsp.tcc.application.repository.CourseModuleRepository;
import br.edu.ifsp.tcc.application.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Team courses (Curso -> Modulo -> Aula, as in the scope document). A lesson can link to the
 * board card that produces it. Every member can view; ADMIN and CREATOR can edit.
 */
@Service
public class CourseService {

    private static final TeamRole[] EDITORS = {TeamRole.ADMIN, TeamRole.CREATOR};

    private final TeamService teamService;
    private final KanbanItemService kanbanItemService;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final CourseLessonRepository lessonRepository;

    public CourseService(TeamService teamService,
                         KanbanItemService kanbanItemService,
                         CourseRepository courseRepository,
                         CourseModuleRepository moduleRepository,
                         CourseLessonRepository lessonRepository) {
        this.teamService = teamService;
        this.kanbanItemService = kanbanItemService;
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
    }

    // --- Courses ---

    @Transactional(readOnly = true)
    public List<CourseResponse> listCourses(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return courseRepository.findByTeam_IdOrderByCreatedAtDesc(teamId).stream()
                .map(course -> toCourseResponse(course, moduleRepository.countByCourse_Id(course.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourse(Long teamId, Long userId, Long courseId) {
        teamService.requireMember(teamId, userId);
        Course course = findCourse(teamId, courseId);

        List<CourseModule> modules = moduleRepository.findByCourse_IdOrderBySortOrderAscIdAsc(courseId);
        Map<Long, List<CourseLesson>> lessonsByModule = modules.isEmpty() ? Map.of()
                : lessonRepository.findByModule_IdInOrderBySortOrderAscIdAsc(modules.stream().map(CourseModule::getId).toList())
                        .stream().collect(Collectors.groupingBy(CourseLesson::getModuleId));

        List<CourseModuleResponse> moduleResponses = modules.stream()
                .map(module -> new CourseModuleResponse(module.getId(), courseId, module.getTitle(), module.getSortOrder(),
                        lessonsByModule.getOrDefault(module.getId(), List.of()).stream().map(this::toLessonResponse).toList(),
                        module.getCreatedAt()))
                .toList();
        return new CourseDetailResponse(course.getId(), teamId, course.getTitle(), course.getDescription(),
                modules.size(), moduleResponses, course.getCreatedAt());
    }

    @Transactional
    public CourseResponse createCourse(Long teamId, Long userId, CreateCourseRequest request) {
        Team team = teamService.requireRole(teamId, userId, EDITORS).getTeam();
        Course course = new Course();
        course.setTeam(team);
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        return toCourseResponse(courseRepository.save(course), 0);
    }

    @Transactional
    public void deleteCourse(Long teamId, Long userId, Long courseId) {
        teamService.requireRole(teamId, userId, EDITORS);
        courseRepository.delete(findCourse(teamId, courseId));
    }

    // --- Modules ---

    @Transactional
    public CourseModuleResponse createModule(Long teamId, Long userId, Long courseId, CreateCourseModuleRequest request) {
        teamService.requireRole(teamId, userId, EDITORS);
        Course course = findCourse(teamId, courseId);

        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(request.title().trim());
        module.setSortOrder(moduleRepository.countByCourse_Id(courseId));
        CourseModule saved = moduleRepository.save(module);
        return new CourseModuleResponse(saved.getId(), courseId, saved.getTitle(), saved.getSortOrder(), List.of(), saved.getCreatedAt());
    }

    @Transactional
    public void deleteModule(Long teamId, Long userId, Long courseId, Long moduleId) {
        teamService.requireRole(teamId, userId, EDITORS);
        findCourse(teamId, courseId);
        moduleRepository.delete(findModule(courseId, moduleId));
    }

    // --- Lessons ---

    @Transactional
    public CourseLessonResponse createLesson(Long teamId, Long userId, Long courseId, Long moduleId,
                                             CreateCourseLessonRequest request) {
        teamService.requireRole(teamId, userId, EDITORS);
        findCourse(teamId, courseId);
        CourseModule module = findModule(courseId, moduleId);

        CourseLesson lesson = new CourseLesson();
        lesson.setModule(module);
        lesson.setTitle(request.title().trim());
        lesson.setType(request.type() != null ? request.type() : LessonType.VIDEO);
        lesson.setProject(findProject(teamId, request.projectId()));
        lesson.setSortOrder(lessonRepository.countByModule_Id(moduleId));
        return toLessonResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public CourseLessonResponse updateLesson(Long teamId, Long userId, Long courseId, Long moduleId, Long lessonId,
                                             UpdateCourseLessonRequest request) {
        teamService.requireRole(teamId, userId, EDITORS);
        findCourse(teamId, courseId);
        findModule(courseId, moduleId);
        CourseLesson lesson = findLesson(moduleId, lessonId);

        if (request.title() != null && !request.title().isBlank()) lesson.setTitle(request.title().trim());
        if (request.type() != null) lesson.setType(request.type());
        if (request.published() != null) lesson.setPublished(request.published());
        if (request.projectId() != null) lesson.setProject(findProject(teamId, request.projectId()));
        return toLessonResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public void deleteLesson(Long teamId, Long userId, Long courseId, Long moduleId, Long lessonId) {
        teamService.requireRole(teamId, userId, EDITORS);
        findCourse(teamId, courseId);
        findModule(courseId, moduleId);
        lessonRepository.delete(findLesson(moduleId, lessonId));
    }

    // --- Helpers ---

    private Course findCourse(Long teamId, Long courseId) {
        return courseRepository.findByIdAndTeam_Id(courseId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso nao encontrado."));
    }

    private CourseModule findModule(Long courseId, Long moduleId) {
        return moduleRepository.findByIdAndCourse_Id(moduleId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modulo nao encontrado."));
    }

    private CourseLesson findLesson(Long moduleId, Long lessonId) {
        return lessonRepository.findByIdAndModule_Id(lessonId, moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula nao encontrada."));
    }

    /** The linked card must belong to the same team. */
    private KanbanItem findProject(Long teamId, Long projectId) {
        if (projectId == null) return null;
        return kanbanItemService.findByIdAndTeamId(projectId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O projeto vinculado nao pertence a esta equipe."));
    }

    private CourseResponse toCourseResponse(Course course, int modulesCount) {
        return new CourseResponse(course.getId(), course.getTeamId(), course.getTitle(), course.getDescription(),
                modulesCount, course.getCreatedAt());
    }

    private CourseLessonResponse toLessonResponse(CourseLesson lesson) {
        return new CourseLessonResponse(lesson.getId(), lesson.getModuleId(), lesson.getTitle(), lesson.getType(),
                Boolean.TRUE.equals(lesson.getPublished()), lesson.getSortOrder() != null ? lesson.getSortOrder() : 0,
                lesson.getProjectId(), lesson.getCreatedAt());
    }
}
