package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        return courseService.toDetailResponse(teamId, courseService.findCourse(teamId, courseId));
    }
}
