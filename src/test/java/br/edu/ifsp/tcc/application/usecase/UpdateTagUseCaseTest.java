package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.dto.UpdateTagDTO;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TagService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateTagUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private TagService tagService;
    @InjectMocks private UpdateTagUseCase useCase;

    private Tag tag() {
        Tag tag = new Tag();
        tag.setId(3L);
        tag.setName("Podcast");
        tag.setColor("#1565C0");
        return tag;
    }

    @Test
    void execute_shouldChangeNameAndColor() {
        Tag tag = tag();
        TagResponse mapped = new TagResponse(3L, "Audio", "#000000");
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(new TeamMember());
        when(tagService.findTag(10L, 3L)).thenReturn(tag);
        when(tagService.nameTaken(10L, "Audio", 3L)).thenReturn(false);
        when(tagService.save(tag)).thenReturn(tag);
        when(tagService.toTagResponse(tag)).thenReturn(mapped);
        UpdateTagDTO dto = new UpdateTagDTO();
        dto.setName(" Audio ");
        dto.setColor("#000000");

        assertSame(mapped, useCase.execute(10L, 1L, 3L, dto));

        assertEquals("Audio", tag.getName());
        assertEquals("#000000", tag.getColor());
    }

    @Test
    void execute_shouldKeepFieldsThatWereNotSent() {
        Tag tag = tag();
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(new TeamMember());
        when(tagService.findTag(10L, 3L)).thenReturn(tag);
        when(tagService.save(tag)).thenReturn(tag);
        UpdateTagDTO dto = new UpdateTagDTO();
        dto.setColor("#ABCDEF");

        useCase.execute(10L, 1L, 3L, dto);

        assertEquals("Podcast", tag.getName());
        assertEquals("#ABCDEF", tag.getColor());
        verify(tagService, never()).nameTaken(any(), any(), any());
    }

    @Test
    void execute_shouldRejectNameUsedByAnotherTag() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(new TeamMember());
        when(tagService.findTag(10L, 3L)).thenReturn(tag());
        when(tagService.nameTaken(10L, "Curso", 3L)).thenReturn(true);
        UpdateTagDTO dto = new UpdateTagDTO();
        dto.setName("Curso");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(tagService, never()).save(any());
    }

    @Test
    void execute_shouldRejectBlankName() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(new TeamMember());
        when(tagService.findTag(10L, 3L)).thenReturn(tag());
        UpdateTagDTO dto = new UpdateTagDTO();
        dto.setName("  ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tagService, never()).save(any());
    }

    @Test
    void execute_shouldReturnNotFoundForTagOfAnotherTeam() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(new TeamMember());
        when(tagService.findTag(10L, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Etiqueta não encontrada."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 99L, new UpdateTagDTO()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void execute_shouldBeBlockedForNonAdmin() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        UpdateTagDTO dto = new UpdateTagDTO();
        dto.setName("X");

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, 3L, dto));
        verifyNoInteractions(tagService);
    }
}
