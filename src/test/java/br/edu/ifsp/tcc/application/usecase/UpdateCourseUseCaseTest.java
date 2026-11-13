package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.dto.UpdateCourseDTO;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCourseUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private UpdateCourseUseCase useCase;

    private Course course() {
        Course course = new Course();
        course.setTitle("Antigo");
        course.setDescription("Descricao");
        when(courseService.findCourse(10L, 20L)).thenReturn(course);
        return course;
    }

    @Test
    void execute_shouldChangeOnlyTheFieldsSent() {
        Course course = course();
        UpdateCourseDTO dto = new UpdateCourseDTO();
        dto.setTitle("  Novo  ");
        CourseResponse response = mock(CourseResponse.class);
        when(courseService.saveCourse(course)).thenReturn(course);
        when(courseService.toCourseResponse(course)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 20L, dto));

        assertEquals("Novo", course.getTitle());
        assertEquals("Descricao", course.getDescription());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void execute_shouldClearTheDescriptionWithAnEmptyText() {
        Course course = course();
        UpdateCourseDTO dto = new UpdateCourseDTO();
        dto.setDescription("");
        when(courseService.saveCourse(course)).thenReturn(course);

        useCase.execute(10L, 1L, 20L, dto);

        assertNull(course.getDescription());
        assertEquals("Antigo", course.getTitle());
    }

    @Test
    void execute_shouldSetANewDescription() {
        Course course = course();
        UpdateCourseDTO dto = new UpdateCourseDTO();
        dto.setDescription(" Nova ");
        when(courseService.saveCourse(course)).thenReturn(course);

        useCase.execute(10L, 1L, 20L, dto);

        assertEquals("Nova", course.getDescription());
    }

    @Test
    void execute_shouldRefuseABlankTitle() {
        course();
        UpdateCourseDTO dto = new UpdateCourseDTO();
        dto.setTitle("   ");

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, dto));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals("O título do curso não pode ser vazio.", e.getReason());
        verify(courseService, never()).saveCourse(any());
    }

    @Test
    void execute_shouldStopWhenTheRoleIsNotAnEditor() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(teamService)
                .requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, new UpdateCourseDTO()));

        verifyNoInteractions(courseService);
    }
}
