package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class GetCourseUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public GetCourseUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    /** Course with its modules and lessons in order. */
    @Transactional(readOnly = true)
    public CourseDetailResponse execute(Long teamId, Long userId, Long courseId) {
        teamService.requireMember(teamId, userId);
        Course course = courseService.findCourse(teamId, courseId);

        List<CourseModule> modules = courseService.findModules(courseId);
        Map<Long, List<CourseLesson>> lessonsByModule = courseService.findLessonsByModule(modules);
        List<CourseModuleResponse> moduleResponses = modules.stream()
                .map(module -> courseService.toModuleResponse(module, lessonsByModule.getOrDefault(module.getId(), List.of())))
                .toList();

        return new CourseDetailResponse(course.getId(), teamId, course.getTitle(), course.getDescription(),
                modules.size(), moduleResponses, course.getCreatedAt());
    }
}
