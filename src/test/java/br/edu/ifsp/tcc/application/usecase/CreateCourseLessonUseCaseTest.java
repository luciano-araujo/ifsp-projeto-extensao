package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateCourseLessonDTO;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.LessonType;
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
class CreateCourseLessonUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private CreateCourseLessonUseCase useCase;

    private CreateCourseLessonDTO dto(Long projectId) {
        CreateCourseLessonDTO dto = new CreateCourseLessonDTO();
        dto.setTitle("Busca em largura");
        dto.setType(null);
        dto.setProjectId(projectId);
        return dto;
    }

    @Test
    void execute_shouldRejectProjectFromAnotherTeam() {
        when(courseService.findModule(20L, 30L)).thenReturn(new CourseModule());
        when(courseService.findProject(10L, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "O projeto vinculado nao pertence a esta equipe."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 20L, 30L, dto(99L)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(courseService, never()).saveLesson(any());
    }

    @Test
    void execute_shouldLinkTeamProjectAndDefaultToVideo() {
        CourseModule module = new CourseModule();
        KanbanItem project = new KanbanItem();
        when(courseService.findModule(20L, 30L)).thenReturn(module);
        when(courseService.findProject(10L, 5L)).thenReturn(project);
        when(courseService.countLessons(30L)).thenReturn(2);

        useCase.execute(10L, 1L, 20L, 30L, dto(5L));

        ArgumentCaptor<CourseLesson> saved = ArgumentCaptor.forClass(CourseLesson.class);
        verify(courseService).saveLesson(saved.capture());
        assertSame(project, saved.getValue().getProject());
        assertSame(module, saved.getValue().getModule());
        assertEquals(LessonType.VIDEO, saved.getValue().getType());
        assertEquals(2, saved.getValue().getSortOrder());
        assertFalse(saved.getValue().getPublished());
    }
}
