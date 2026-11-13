package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.dto.CourseExportFile;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.service.CourseExportService;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Component
public class ExportCourseUseCase {

    private final TeamService teamService;
    private final CourseService courseService;
    private final CourseExportService exportService;

    public ExportCourseUseCase(TeamService teamService, CourseService courseService, CourseExportService exportService) {
        this.teamService = teamService;
        this.courseService = courseService;
        this.exportService = exportService;
    }

    /** Exports the course tree as json (default) or csv; any team member may export. */
    @Transactional(readOnly = true)
    public CourseExportFile execute(Long teamId, Long userId, Long courseId, String format) {
        teamService.requireMember(teamId, userId);
        String chosen = format == null || format.isBlank() ? "json" : format.trim().toLowerCase(Locale.ROOT);
        if (!chosen.equals("json") && !chosen.equals("csv")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato inválido. Use json ou csv.");
        }
        Course course = courseService.findCourse(teamId, courseId);
        CourseDetailResponse tree = courseService.toDetailResponse(teamId, course);
        String name = "curso-" + course.getId() + "." + chosen;
        return chosen.equals("csv")
                ? new CourseExportFile(name, "text/csv; charset=UTF-8", exportService.toCsv(tree))
                : new CourseExportFile(name, "application/json", tree);
    }
}
