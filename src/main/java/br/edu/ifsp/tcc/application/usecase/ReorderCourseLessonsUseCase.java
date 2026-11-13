package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.dto.ReorderLessonsDTO;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
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
public class ReorderCourseLessonsUseCase {

    private final TeamService teamService;
    private final CourseService courseService;

    public ReorderCourseLessonsUseCase(TeamService teamService, CourseService courseService) {
        this.teamService = teamService;
        this.courseService = courseService;
    }

    /** The list must contain exactly the lessons of the module; returns them in the new order. */
    @Transactional
    public List<CourseLessonResponse> execute(Long teamId, Long userId, Long courseId, Long moduleId, ReorderLessonsDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        courseService.findModule(courseId, moduleId);
        List<CourseLesson> lessons = courseService.findLessons(moduleId);
        courseService.requireExactIds(dto.getLessonIds(), lessons.stream().map(CourseLesson::getId).toList(),
                "A lista deve conter exatamente as aulas do módulo.");

        Map<Long, CourseLesson> byId = lessons.stream().collect(Collectors.toMap(CourseLesson::getId, Function.identity()));
        List<CourseLesson> ordered = new ArrayList<>();
        for (int i = 0; i < dto.getLessonIds().size(); i++) {
            CourseLesson lesson = byId.get(dto.getLessonIds().get(i));
            lesson.setSortOrder(i);
            ordered.add(courseService.saveLesson(lesson));
        }
        Map<Long, List<MaterialResponse>> materials = courseService.findMaterialsByLesson(teamId, Map.of(moduleId, ordered));
        return ordered.stream()
                .map(l -> courseService.toLessonResponse(l, materials.getOrDefault(l.getId(), List.of())))
                .toList();
    }
}
