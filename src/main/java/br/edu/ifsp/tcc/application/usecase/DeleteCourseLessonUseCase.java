package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteCourseLessonUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public DeleteCourseLessonUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    @Transactional
    public void execute(Long teamId, Long userId, Long courseId, Long moduleId, Long lessonId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        courseService.findModule(courseId, moduleId);
        courseService.deleteLesson(courseService.findLesson(moduleId, lessonId));
    }
}
