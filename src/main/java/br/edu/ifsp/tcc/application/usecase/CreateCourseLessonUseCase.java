package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.CreateCourseLessonDTO;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.LessonType;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateCourseLessonUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public CreateCourseLessonUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    /** Adds a lesson to a module, optionally linked to the team card that produces it. */
    @Transactional
    public CourseLessonResponse execute(Long teamId, Long userId, Long courseId, Long moduleId, CreateCourseLessonDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        CourseModule module = courseService.findModule(courseId, moduleId);

        CourseLesson lesson = new CourseLesson();
        lesson.setModule(module);
        lesson.setTitle(dto.getTitle().trim());
        lesson.setType(dto.getType() != null ? dto.getType() : LessonType.VIDEO);
        lesson.setProject(courseService.findProject(teamId, dto.getProjectId()));
        lesson.setSortOrder(courseService.countLessons(moduleId));
        return courseService.toLessonResponse(courseService.saveLesson(lesson));
    }
}
