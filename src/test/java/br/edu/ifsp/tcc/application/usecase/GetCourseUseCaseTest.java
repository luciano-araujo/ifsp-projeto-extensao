package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCourseUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private GetCourseUseCase useCase;

    private CourseModule module(Long id) {
        CourseModule module = new CourseModule();
        module.setId(id);
        return module;
    }

    @Test
    void execute_shouldReturnModulesWithTheirLessons() {
        Course course = new Course();
        course.setId(20L);
        course.setTitle("Algoritmos");
        CourseModule grafos = module(30L);
        CourseModule arvores = module(31L);
        List<CourseLesson> grafosLessons = List.of(new CourseLesson());
        when(courseService.findCourse(10L, 20L)).thenReturn(course);
        when(courseService.findModules(20L)).thenReturn(List.of(grafos, arvores));
        when(courseService.findLessonsByModule(List.of(grafos, arvores))).thenReturn(Map.of(30L, grafosLessons));

        CourseDetailResponse detail = useCase.execute(10L, 1L, 20L);

        assertEquals("Algoritmos", detail.getTitle());
        assertEquals(2, detail.getModulesCount());
        verify(teamService).requireMember(10L, 1L);
        verify(courseService).toModuleResponse(grafos, grafosLessons);
        verify(courseService).toModuleResponse(arvores, List.of());
    }
}
