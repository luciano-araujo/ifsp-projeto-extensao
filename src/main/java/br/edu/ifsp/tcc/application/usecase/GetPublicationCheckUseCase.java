package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.PublicationCheckResponse;
import br.edu.ifsp.tcc.application.service.CoursePublicationService;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetPublicationCheckUseCase {

    private final TeamService teamService;
    private final CourseService courseService;
    private final CoursePublicationService publicationService;

    public GetPublicationCheckUseCase(TeamService teamService, CourseService courseService,
                                      CoursePublicationService publicationService) {
        this.teamService = teamService;
        this.courseService = courseService;
        this.publicationService = publicationService;
    }

    /** What stops the course from being published (RN03, RN06); any team member may look. */
    @Transactional(readOnly = true)
    public PublicationCheckResponse execute(Long teamId, Long userId, Long courseId) {
        teamService.requireMember(teamId, userId);
        return publicationService.check(teamId, courseService.findCourse(teamId, courseId));
    }
}
