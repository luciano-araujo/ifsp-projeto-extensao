package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.dto.UpdateCourseDTO;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UpdateCourseUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public UpdateCourseUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    /** Changes the title and/or description (editors only); only the fields sent are changed, "" clears the description. */
    @Transactional
    public CourseResponse execute(Long teamId, Long userId, Long courseId, UpdateCourseDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        Course course = courseService.findCourse(teamId, courseId);

        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título do curso não pode ser vazio.");
            }
            course.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            course.setDescription(dto.getDescription().isBlank() ? null : dto.getDescription().trim());
        }
        return courseService.toCourseResponse(courseService.saveCourse(course));
    }
}
