package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.PublicationCheckResponse;
import br.edu.ifsp.tcc.application.entity.Course;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPublicationCheckUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @Mock private CoursePublicationService publicationService;
    @InjectMocks private GetPublicationCheckUseCase useCase;

    @Test
    void execute_shouldAllowAnyMember() {
        Course course = new Course();
        PublicationCheckResponse check = new PublicationCheckResponse(true, List.of());
        when(courseService.findCourse(10L, 20L)).thenReturn(course);
        when(publicationService.check(10L, course)).thenReturn(check);

        assertSame(check, useCase.execute(10L, 1L, 20L));

        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void execute_shouldStopWhenTheCourseIsNotInTheTeam() {
        when(courseService.findCourse(10L, 20L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L));

        verifyNoInteractions(publicationService);
    }
}
