package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CoursePublicationService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishCourseUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @Mock private CoursePublicationService publicationService;
    @InjectMocks private PublishCourseUseCase useCase;

    @Test
    void execute_shouldRequireAnEditorAndReturnTheCourse() {
        Course course = new Course();
        CourseResponse response = mock(CourseResponse.class);
        when(courseService.findCourse(10L, 20L)).thenReturn(course);
        when(publicationService.publish(10L, course)).thenReturn(course);
        when(courseService.toCourseResponse(course)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 20L));

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void execute_shouldPropagateTheProblems() {
        Course course = new Course();
        when(courseService.findCourse(10L, 20L)).thenReturn(course);
        when(publicationService.publish(10L, course)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "x"));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L));
        verify(courseService, never()).toCourseResponse(any());
    }

    @Test
    void execute_shouldStopWhenTheRoleIsNotAnEditor() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(teamService)
                .requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L));

        verifyNoInteractions(courseService, publicationService);
    }
}
