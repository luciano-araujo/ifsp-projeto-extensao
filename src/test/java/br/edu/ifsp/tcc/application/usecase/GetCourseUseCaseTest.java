package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCourseUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private GetCourseUseCase useCase;

    @Test
    void execute_shouldReturnTheCourseTree() {
        Course course = new Course();
        CourseDetailResponse detail = mock(CourseDetailResponse.class);
        when(courseService.findCourse(10L, 20L)).thenReturn(course);
        when(courseService.toDetailResponse(10L, course)).thenReturn(detail);

        assertSame(detail, useCase.execute(10L, 1L, 20L));

        verify(teamService).requireMember(10L, 1L);
    }
}
