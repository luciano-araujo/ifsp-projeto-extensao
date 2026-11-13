package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.MaterialService;
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
class UnlinkLessonMaterialUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @Mock private MaterialService materialService;
    @InjectMocks private UnlinkLessonMaterialUseCase useCase;

    @Test
    void execute_shouldRemoveTheLink() {
        CourseLesson lesson = new CourseLesson();
        lesson.setId(40L);
        when(courseService.findLesson(30L, 40L)).thenReturn(lesson);

        useCase.execute(10L, 1L, 20L, 30L, 40L, 7L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(courseService).findCourse(10L, 20L);
        verify(courseService).findModule(20L, 30L);
        verify(materialService).unlink(40L, 7L);
    }

    @Test
    void execute_shouldBeNotFoundWhenTheMaterialIsNotLinked() {
        CourseLesson lesson = new CourseLesson();
        lesson.setId(40L);
        when(courseService.findLesson(30L, 40L)).thenReturn(lesson);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Este material não está vinculado à aula."))
                .when(materialService).unlink(40L, 7L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 20L, 30L, 40L, 7L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void execute_shouldBeForbiddenForReviewers() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, 30L, 40L, 7L));

        verifyNoInteractions(courseService, materialService);
    }
}
