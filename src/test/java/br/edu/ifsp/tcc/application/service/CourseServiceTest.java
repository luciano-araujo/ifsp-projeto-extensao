package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CourseDTOs.CourseDetailResponse;
import br.edu.ifsp.tcc.application.dto.CourseDTOs.CreateCourseLessonRequest;
import br.edu.ifsp.tcc.application.dto.CourseDTOs.CreateCourseRequest;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.CourseLessonRepository;
import br.edu.ifsp.tcc.application.repository.CourseModuleRepository;
import br.edu.ifsp.tcc.application.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private TeamService teamService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository moduleRepository;
    @Mock private CourseLessonRepository lessonRepository;
    @InjectMocks private CourseService courseService;

    private Team team() {
        Team team = new Team();
        team.setId(10L);
        return team;
    }

    private TeamMember admin(Team team) {
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setRole(TeamRole.ADMIN);
        return member;
    }

    private Course course(Team team) {
        Course course = new Course();
        course.setId(20L);
        course.setTeam(team);
        course.setTitle("Algoritmos");
        return course;
    }

    private CourseModule module(Long id, Course course) {
        CourseModule module = new CourseModule();
        module.setId(id);
        module.setCourse(course);
        module.setTitle("Modulo " + id);
        return module;
    }

    @Test
    void createCourse_shouldSaveCourseInTeam() {
        Team team = team();
        when(teamService.requireRole(eq(10L), eq(1L), any(), any())).thenReturn(admin(team));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = courseService.createCourse(10L, 1L, new CreateCourseRequest("  Algoritmos  ", "Curso piloto"));

        assertEquals("Algoritmos", response.title());
        assertEquals(10L, response.teamId());
        assertEquals(0, response.modulesCount());
    }

    @Test
    void createCourse_shouldBeBlockedForReviewer() {
        when(teamService.requireRole(eq(10L), eq(2L), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe nao permite esta acao."));

        assertThrows(ResponseStatusException.class,
                () -> courseService.createCourse(10L, 2L, new CreateCourseRequest("Curso", null)));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void getCourse_shouldReturnModulesWithTheirLessons() {
        Team team = team();
        Course course = course(team);
        CourseModule grafos = module(30L, course);
        CourseModule arvores = module(31L, course);
        CourseLesson lesson = new CourseLesson();
        lesson.setId(40L);
        lesson.setModule(grafos);
        lesson.setTitle("BFS");
        lesson.setType(LessonType.VIDEO);

        when(courseRepository.findByIdAndTeam_Id(20L, 10L)).thenReturn(Optional.of(course));
        when(moduleRepository.findByCourse_IdOrderBySortOrderAscIdAsc(20L)).thenReturn(List.of(grafos, arvores));
        when(lessonRepository.findByModule_IdInOrderBySortOrderAscIdAsc(anyList())).thenReturn(List.of(lesson));

        CourseDetailResponse detail = courseService.getCourse(10L, 1L, 20L);

        assertEquals(2, detail.modulesCount());
        assertEquals(1, detail.modules().get(0).lessons().size());
        assertEquals("BFS", detail.modules().get(0).lessons().get(0).title());
        assertTrue(detail.modules().get(1).lessons().isEmpty());
        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void createLesson_shouldRejectProjectFromAnotherTeam() {
        Team team = team();
        Course course = course(team);
        when(teamService.requireRole(eq(10L), eq(1L), any(), any())).thenReturn(admin(team));
        when(courseRepository.findByIdAndTeam_Id(20L, 10L)).thenReturn(Optional.of(course));
        when(moduleRepository.findByIdAndCourse_Id(30L, 20L)).thenReturn(Optional.of(module(30L, course)));
        when(kanbanItemService.findByIdAndTeamId(99L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> courseService.createLesson(10L, 1L, 20L, 30L, new CreateCourseLessonRequest("BFS", LessonType.VIDEO, 99L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void createLesson_shouldLinkTeamProjectAndDefaultToVideo() {
        Team team = team();
        Course course = course(team);
        KanbanItem project = new KanbanItem();
        project.setId(5L);
        when(teamService.requireRole(eq(10L), eq(1L), any(), any())).thenReturn(admin(team));
        when(courseRepository.findByIdAndTeam_Id(20L, 10L)).thenReturn(Optional.of(course));
        when(moduleRepository.findByIdAndCourse_Id(30L, 20L)).thenReturn(Optional.of(module(30L, course)));
        when(kanbanItemService.findByIdAndTeamId(5L, 10L)).thenReturn(Optional.of(project));
        when(lessonRepository.save(any(CourseLesson.class))).thenAnswer(inv -> inv.getArgument(0));

        var lesson = courseService.createLesson(10L, 1L, 20L, 30L, new CreateCourseLessonRequest("BFS", null, 5L));

        assertEquals(5L, lesson.projectId());
        assertEquals(LessonType.VIDEO, lesson.type());
        assertFalse(lesson.published());
    }

    @Test
    void deleteCourse_shouldReturnNotFoundForCourseOfAnotherTeam() {
        when(teamService.requireRole(eq(10L), eq(1L), any(), any())).thenReturn(admin(team()));
        when(courseRepository.findByIdAndTeam_Id(20L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> courseService.deleteCourse(10L, 1L, 20L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(courseRepository, never()).delete(any());
    }
}
