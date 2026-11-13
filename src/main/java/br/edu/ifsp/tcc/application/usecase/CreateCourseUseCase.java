package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.dto.CreateCourseDTO;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateCourseUseCase {

    private final TeamService teamService;
    private final CourseService courseService;
    private final PlanService planService;

    public CreateCourseUseCase(TeamService teamService, CourseService courseService, PlanService planService) {
        this.teamService = teamService;
        this.planService = planService;
        this.courseService = courseService;
    }

    @Transactional
    public CourseResponse execute(Long teamId, Long userId, CreateCourseDTO dto) {
        Team team = teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR).getTeam();
        planService.assertCanCreateCourse(team);

        Course course = new Course();
        course.setTeam(team);
        course.setTitle(dto.getTitle().trim());
        course.setDescription(dto.getDescription());
        return courseService.toCourseResponse(courseService.saveCourse(course));
    }
}
