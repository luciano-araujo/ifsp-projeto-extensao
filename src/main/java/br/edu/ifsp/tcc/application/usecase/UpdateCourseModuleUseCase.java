package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.UpdateModuleDTO;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class UpdateCourseModuleUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public UpdateCourseModuleUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    /** Renames a module (editors only); returns it with its lessons. */
    @Transactional
    public CourseModuleResponse execute(Long teamId, Long userId, Long courseId, Long moduleId, UpdateModuleDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        CourseModule module = courseService.findModule(courseId, moduleId);

        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título do módulo não pode ser vazio.");
            }
            module.setTitle(dto.getTitle().trim());
        }
        return courseService.toModuleResponses(teamId, List.of(courseService.saveModule(module))).get(0);
    }
}
