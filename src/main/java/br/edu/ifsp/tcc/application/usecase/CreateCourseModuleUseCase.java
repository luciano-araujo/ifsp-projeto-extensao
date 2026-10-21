package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.CreateModuleDTO;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CreateCourseModuleUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public CreateCourseModuleUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    @Transactional
    public CourseModuleResponse execute(Long teamId, Long userId, Long courseId, CreateModuleDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        Course course = courseService.findCourse(teamId, courseId);

        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(dto.getTitle().trim());
        module.setSortOrder(courseService.countModules(courseId));
        return courseService.toModuleResponse(courseService.saveModule(module), List.of());
    }
}
