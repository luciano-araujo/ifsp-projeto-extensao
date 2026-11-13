package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.ReorderModulesDTO;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReorderCourseModulesUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public ReorderCourseModulesUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    /** The list must contain exactly the modules of the course; returns them (with lessons) in the new order. */
    @Transactional
    public List<CourseModuleResponse> execute(Long teamId, Long userId, Long courseId, ReorderModulesDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        List<CourseModule> modules = courseService.findModules(courseId);
        courseService.requireExactIds(dto.getModuleIds(), modules.stream().map(CourseModule::getId).toList(),
                "A lista deve conter exatamente os módulos do curso.");

        Map<Long, CourseModule> byId = modules.stream().collect(Collectors.toMap(CourseModule::getId, Function.identity()));
        List<CourseModule> ordered = new ArrayList<>();
        for (int i = 0; i < dto.getModuleIds().size(); i++) {
            CourseModule module = byId.get(dto.getModuleIds().get(i));
            module.setSortOrder(i);
            ordered.add(courseService.saveModule(module));
        }
        return courseService.toModuleResponses(teamId, ordered);
    }
}
