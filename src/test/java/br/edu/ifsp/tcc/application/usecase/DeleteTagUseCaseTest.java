package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Tag;
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
class DeleteTagUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private TagService tagService;
    @InjectMocks private DeleteTagUseCase useCase;

    @Test
    void execute_shouldDeleteTagOfTheTeam() {
        Tag tag = new Tag();
        when(tagService.findTag(10L, 3L)).thenReturn(tag);

        useCase.execute(10L, 1L, 3L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
        verify(tagService).delete(tag);
    }

    @Test
    void execute_shouldReturnNotFoundForTagOfAnotherTeam() {
        when(tagService.findTag(10L, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Etiqueta não encontrada."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 99L));
        verify(tagService, never()).delete(any());
    }

    @Test
    void execute_shouldBeBlockedForNonAdmin() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 2L, 3L));
        verifyNoInteractions(tagService);
    }
}
