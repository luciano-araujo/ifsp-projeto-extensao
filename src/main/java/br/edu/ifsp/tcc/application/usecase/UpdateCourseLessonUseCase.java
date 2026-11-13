package br.edu.ifsp.tcc.application.usecase;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
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

    /** Publishes a lesson or changes its title, type, description, link or linked card; only the fields sent are changed. */
    @Transactional
    public CourseLessonResponse execute(Long teamId, Long userId, Long courseId, Long moduleId, Long lessonId,
                                        UpdateCourseLessonDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        courseService.findModule(courseId, moduleId);
        CourseLesson lesson = courseService.findLesson(moduleId, lessonId);

        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título da aula não pode ser vazio.");
            }
            lesson.setTitle(dto.getTitle().trim());
        }
        if (dto.getType() != null) lesson.setType(dto.getType());
        if (dto.getPublished() != null) lesson.setPublished(dto.getPublished());
        if (dto.getDescription() != null) lesson.setDescription(CourseService.blankToNull(dto.getDescription()));
        if (dto.getContentUrl() != null) lesson.setContentUrl(CourseService.blankToNull(dto.getContentUrl()));
        if (dto.getProjectId() != null) lesson.setProject(courseService.findProject(teamId, dto.getProjectId()));
        return courseService.toLessonResponseWithMaterials(teamId, courseService.saveLesson(lesson));
    }
}
