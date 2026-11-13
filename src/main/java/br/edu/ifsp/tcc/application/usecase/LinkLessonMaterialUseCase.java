package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.LinkLessonMaterialDTO;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.Material;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LinkLessonMaterialUseCase {

    private final TeamService teamService;
    private final CourseService courseService;
    private final MaterialService materialService;

    public LinkLessonMaterialUseCase(TeamService teamService, CourseService courseService, MaterialService materialService) {
        this.teamService = teamService;
        this.courseService = courseService;
        this.materialService = materialService;
    }

    /** Links a material of the same team to the lesson (editors only); another team's or unknown material is a 400, linking twice a 409. */
    @Transactional
    public CourseLessonResponse execute(Long teamId, Long userId, Long courseId, Long moduleId, Long lessonId,
                                        LinkLessonMaterialDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        courseService.findCourse(teamId, courseId);
        courseService.findModule(courseId, moduleId);
        CourseLesson lesson = courseService.findLesson(moduleId, lessonId);
        Material material = materialService.findMaterialToLink(teamId, dto.getMaterialId());

        materialService.link(lesson, material);
        return courseService.toLessonResponseWithMaterials(teamId, lesson);
    }
}
