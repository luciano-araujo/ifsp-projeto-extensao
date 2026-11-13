package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.LinkLessonMaterialDTO;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.Material;
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
class LinkLessonMaterialUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @Mock private MaterialService materialService;
    @InjectMocks private LinkLessonMaterialUseCase useCase;

    private LinkLessonMaterialDTO dto() {
        LinkLessonMaterialDTO dto = new LinkLessonMaterialDTO();
        dto.setMaterialId(7L);
        return dto;
    }

    @Test
    void execute_shouldLinkTheMaterialAndReturnTheLessonWithItsMaterials() {
        CourseLesson lesson = new CourseLesson();
        Material material = new Material();
        CourseLessonResponse response = mock(CourseLessonResponse.class);
        when(courseService.findLesson(30L, 40L)).thenReturn(lesson);
        when(materialService.findMaterialToLink(10L, 7L)).thenReturn(material);
        when(courseService.toLessonResponseWithMaterials(10L, lesson)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 20L, 30L, 40L, dto()));

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(courseService).findCourse(10L, 20L);
        verify(courseService).findModule(20L, 30L);
        verify(materialService).link(lesson, material);
    }

    @Test
    void execute_shouldBeBadRequestForAMaterialOfAnotherTeam() {
        when(courseService.findLesson(30L, 40L)).thenReturn(new CourseLesson());
        when(materialService.findMaterialToLink(10L, 7L))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "O material não pertence a esta equipe."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 20L, 30L, 40L, dto()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("O material não pertence a esta equipe.", ex.getReason());
        verify(materialService, never()).link(any(), any());
    }

    @Test
    void execute_shouldBeConflictWhenAlreadyLinked() {
        CourseLesson lesson = new CourseLesson();
        Material material = new Material();
        when(courseService.findLesson(30L, 40L)).thenReturn(lesson);
        when(materialService.findMaterialToLink(10L, 7L)).thenReturn(material);
        when(materialService.link(lesson, material))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Este material já está vinculado à aula."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 20L, 30L, 40L, dto()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(courseService, never()).toLessonResponseWithMaterials(any(), any());
    }

    @Test
    void execute_shouldBeNotFoundForALessonOfAnotherModule() {
        when(courseService.findLesson(30L, 40L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, 30L, 40L, dto()));

        verifyNoInteractions(materialService);
    }

    @Test
    void execute_shouldBeForbiddenForReviewers() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, 30L, 40L, dto()));

        verifyNoInteractions(courseService, materialService);
    }
}
