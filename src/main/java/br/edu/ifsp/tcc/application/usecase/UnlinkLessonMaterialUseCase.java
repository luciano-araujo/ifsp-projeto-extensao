package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UnlinkLessonMaterialUseCase {

    private final TeamService teamService;
    private final CourseService courseService;
    private final MaterialService materialService;

    public UnlinkLessonMaterialUseCase(TeamService teamService, CourseService courseService, MaterialService materialService) {
        this.teamService = teamService;
        this.courseService = courseService;
        this.materialService = materialService;
    }

    /** Removes the link (editors only). The material itself stays on its card. */
    @Transactional
    public void execute(Long teamId, Long userId, Long courseId, Long moduleId, Long lessonId, Long materialId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        courseService.findModule(courseId, moduleId);
        CourseLesson lesson = courseService.findLesson(moduleId, lessonId);
        materialService.unlink(lesson.getId(), materialId);
    }
}
