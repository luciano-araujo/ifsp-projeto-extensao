package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.UpdateCourseLessonDTO;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UpdateCourseLessonUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public UpdateCourseLessonUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    /** Publishes a lesson or changes its title, type or linked card; only the fields sent are changed. */
    @Transactional
    public CourseLessonResponse execute(Long teamId, Long userId, Long courseId, Long moduleId, Long lessonId,
                                        UpdateCourseLessonDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        courseService.findModule(courseId, moduleId);
        CourseLesson lesson = courseService.findLesson(moduleId, lessonId);

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) lesson.setTitle(dto.getTitle().trim());
        if (dto.getType() != null) lesson.setType(dto.getType());
        if (dto.getPublished() != null) lesson.setPublished(dto.getPublished());
        if (dto.getProjectId() != null) lesson.setProject(courseService.findProject(teamId, dto.getProjectId()));
        return courseService.toLessonResponse(courseService.saveLesson(lesson));
    }
}
