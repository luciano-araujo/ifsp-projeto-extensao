package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateCourseDTO;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCourseUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private CreateCourseUseCase useCase;

    private CreateCourseDTO dto(String title) {
        CreateCourseDTO dto = new CreateCourseDTO();
        dto.setTitle(title);
        dto.setDescription("Curso piloto");
        return dto;
    }

    @Test
    void execute_shouldSaveCourseInTeam() {
        Team team = new Team();
        TeamMember admin = new TeamMember();
        admin.setTeam(team);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(admin);

        useCase.execute(10L, 1L, dto("  Algoritmos  "));

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseService).saveCourse(saved.capture());
        assertEquals("Algoritmos", saved.getValue().getTitle());
        assertSame(team, saved.getValue().getTeam());
    }

    @Test
    void execute_shouldBeBlockedForReviewer() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe nao permite esta acao."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, dto("Curso")));
        verify(courseService, never()).saveCourse(any());
    }
}
