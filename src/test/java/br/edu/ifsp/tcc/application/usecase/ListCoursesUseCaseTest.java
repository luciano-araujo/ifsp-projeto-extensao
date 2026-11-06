package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListCoursesUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private ListCoursesUseCase useCase;

    @Test
    void execute_shouldMapCoursesOfTeam() {
        Course course = new Course();
        CourseResponse response = mock(CourseResponse.class);
        when(courseService.findCourses(10L)).thenReturn(List.of(course));
        when(courseService.toCourseResponse(course)).thenReturn(response);

        List<CourseResponse> result = useCase.execute(10L, 1L);

        assertEquals(List.of(response), result);
        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void execute_shouldReturnEmptyListWhenTeamHasNoCourses() {
        when(courseService.findCourses(10L)).thenReturn(List.of());

        assertTrue(useCase.execute(10L, 1L).isEmpty());
    }
}
