package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.UpdateModuleDTO;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCourseModuleUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private UpdateCourseModuleUseCase useCase;

    @Test
    void execute_shouldRenameTheModule() {
        CourseModule module = new CourseModule();
        module.setTitle("Antigo");
        UpdateModuleDTO dto = new UpdateModuleDTO();
        dto.setTitle(" Novo ");
        CourseModuleResponse response = mock(CourseModuleResponse.class);
        when(courseService.findModule(20L, 30L)).thenReturn(module);
        when(courseService.saveModule(module)).thenReturn(module);
        when(courseService.toModuleResponses(10L, List.of(module))).thenReturn(List.of(response));

        assertSame(response, useCase.execute(10L, 1L, 20L, 30L, dto));

        assertEquals("Novo", module.getTitle());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(courseService).findCourse(10L, 20L);
    }

    @Test
    void execute_shouldKeepTheTitleWhenNoneIsSent() {
        CourseModule module = new CourseModule();
        module.setTitle("Antigo");
        when(courseService.findModule(20L, 30L)).thenReturn(module);
        when(courseService.saveModule(module)).thenReturn(module);
        when(courseService.toModuleResponses(10L, List.of(module))).thenReturn(List.of(mock(CourseModuleResponse.class)));

        useCase.execute(10L, 1L, 20L, 30L, new UpdateModuleDTO());

        assertEquals("Antigo", module.getTitle());
    }

    @Test
    void execute_shouldRefuseABlankTitle() {
        when(courseService.findModule(20L, 30L)).thenReturn(new CourseModule());
        UpdateModuleDTO dto = new UpdateModuleDTO();
        dto.setTitle(" ");

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, 30L, dto));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals("O título do módulo não pode ser vazio.", e.getReason());
    }
}
