package br.edu.ifsp.tcc.application.usecase;

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
class DeleteCourseUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private DeleteCourseUseCase useCase;

    @Test
    void execute_shouldDeleteCourseOfTheTeam() {
        Course course = new Course();
        when(courseService.findCourse(10L, 20L)).thenReturn(course);

        useCase.execute(10L, 1L, 20L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(courseService).deleteCourse(course);
    }

    @Test
    void execute_shouldReturnNotFoundForCourseOfAnotherTeam() {
        when(courseService.findCourse(10L, 20L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso nao encontrado."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(courseService, never()).deleteCourse(any());
    }
}
