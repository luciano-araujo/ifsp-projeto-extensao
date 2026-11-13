package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CoursePublicationService;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PublishCourseUseCase {

    private final TeamService teamService;
    private final CourseService courseService;
    private final CoursePublicationService publicationService;

    public PublishCourseUseCase(TeamService teamService, CourseService courseService,
                                CoursePublicationService publicationService) {
        this.teamService = teamService;
        this.courseService = courseService;
        this.publicationService = publicationService;
    }

    /** Publishes the course (editors only); 400 with the problems when it is not ready (RN03, RN06). */
    @Transactional
    public CourseResponse execute(Long teamId, Long userId, Long courseId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        return courseService.toCourseResponse(publicationService.publish(teamId, courseService.findCourse(teamId, courseId)));
    }
}
