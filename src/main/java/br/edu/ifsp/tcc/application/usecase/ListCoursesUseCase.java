package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListCoursesUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public ListCoursesUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> execute(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return courseService.findCourses(teamId).stream()
                .map(courseService::toCourseResponse)
                .toList();
    }
}
