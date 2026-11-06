package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.CreateModuleDTO;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCourseModuleUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private CreateCourseModuleUseCase useCase;

    @Test
    void execute_shouldCreateModuleAtEndOfCourse() {
        CreateModuleDTO dto = new CreateModuleDTO();
        dto.setTitle("  Introducao ");
        Course course = new Course();
        CourseModule saved = new CourseModule();
        CourseModuleResponse response = mock(CourseModuleResponse.class);
        when(courseService.findCourse(10L, 2L)).thenReturn(course);
        when(courseService.countModules(2L)).thenReturn(1);
        when(courseService.saveModule(any(CourseModule.class))).thenReturn(saved);
        when(courseService.toModuleResponse(saved, List.of())).thenReturn(response);

        CourseModuleResponse result = useCase.execute(10L, 1L, 2L, dto);

        assertSame(response, result);
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(courseService).saveModule(argThat(module ->
                module.getCourse() == course && "Introducao".equals(module.getTitle()) && module.getSortOrder() == 1));
    }
}
