package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateTagDTO;
import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TagService;
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
class CreateTagUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private TagService tagService;
    @InjectMocks private CreateTagUseCase useCase;

    private CreateTagDTO dto(String name) {
        CreateTagDTO dto = new CreateTagDTO();
        dto.setName(name);
        dto.setColor("#112233");
        return dto;
    }

    @Test
    void execute_shouldCreateTagInTheTeam() {
        Team team = new Team();
        TeamMember admin = new TeamMember();
        admin.setTeam(team);
        TagResponse mapped = new TagResponse(7L, "Live", "#112233");
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(tagService.nameTaken(10L, "Live", null)).thenReturn(false);
        when(tagService.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tagService.toTagResponse(any(Tag.class))).thenReturn(mapped);

        TagResponse result = useCase.execute(10L, 1L, dto("  Live "));

        assertSame(mapped, result);
        ArgumentCaptor<Tag> saved = ArgumentCaptor.forClass(Tag.class);
        verify(tagService).save(saved.capture());
        assertEquals("Live", saved.getValue().getName());
        assertEquals("#112233", saved.getValue().getColor());
        assertSame(team, saved.getValue().getTeam());
    }

    @Test
    void execute_shouldRejectDuplicateNameWithConflict() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(new TeamMember());
        when(tagService.nameTaken(10L, "Podcast", null)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("Podcast")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(tagService, never()).save(any());
    }

    @Test
    void execute_shouldRejectBlankName() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(new TeamMember());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("   ")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(tagService, never()).save(any());
    }

    @Test
    void execute_shouldBeBlockedForNonAdmin() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 2L, dto("Live")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(tagService);
    }
}
