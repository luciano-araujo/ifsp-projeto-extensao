package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.ReorderModulesDTO;
import br.edu.ifsp.tcc.application.entity.CourseModule;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReorderCourseModulesUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private ReorderCourseModulesUseCase useCase;

    private CourseModule module(Long id) {
        CourseModule module = new CourseModule();
        module.setId(id);
        return module;
    }

    private ReorderModulesDTO dto(Long... ids) {
        ReorderModulesDTO dto = new ReorderModulesDTO();
        dto.setModuleIds(List.of(ids));
        return dto;
    }

    @Test
    void execute_shouldRenumberTheModulesInTheRequestedOrder() {
        CourseModule a = module(1L);
        CourseModule b = module(2L);
        CourseModule c = module(3L);
        when(courseService.findModules(20L)).thenReturn(List.of(a, b, c));
        when(courseService.saveModule(any(CourseModule.class))).thenAnswer(inv -> inv.getArgument(0));
        List<CourseModuleResponse> responses = List.of(mock(CourseModuleResponse.class));
        when(courseService.toModuleResponses(10L, List.of(c, a, b))).thenReturn(responses);

        assertSame(responses, useCase.execute(10L, 1L, 20L, dto(3L, 1L, 2L)));

        assertEquals(0, c.getSortOrder());
        assertEquals(1, a.getSortOrder());
        assertEquals(2, b.getSortOrder());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(courseService).requireExactIds(List.of(3L, 1L, 2L), List.of(1L, 2L, 3L),
                "A lista deve conter exatamente os módulos do curso.");
    }

    @Test
    void execute_shouldNotSaveWhenTheListIsWrong() {
        when(courseService.findModules(20L)).thenReturn(List.of(module(1L)));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "x")).when(courseService)
                .requireExactIds(anyList(), anyList(), anyString());

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, dto(1L, 2L)));

        verify(courseService, never()).saveModule(any());
    }
}
