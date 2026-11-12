package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestoreScriptVersionUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private TeamService teamService;
    @InjectMocks private RestoreScriptVersionUseCase useCase;

    private static final String FORBIDDEN = "Seu papel na equipe não permite esta ação.";

    private KanbanItem card() {
        Team team = new Team();
        team.setId(10L);
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTeam(team);
        return item;
    }

    private void notFound(Long userId) {
        when(kanbanItemService.findCardOfMember(1L, userId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Card não encontrado."));
    }

    private void reviewer(KanbanItem item, Long userId) {
        when(kanbanItemService.findCardOfMember(1L, userId)).thenReturn(item);
        when(teamService.requireRole(10L, userId, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, FORBIDDEN));
    }

    private Script script(Long id) {
        Script script = new Script();
        script.setId(id);
        script.setContent("<p>Current</p>");
        return script;
    }

    private ScriptVersion version(Script owner) {
        ScriptVersion version = new ScriptVersion();
        version.setId(5L);
        version.setScript(owner);
        version.setContent("<p>Old version</p>");
        return version;
    }

    @Test
    void execute_shouldRestoreContentFromVersion() {
        Script script = script(10L);
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(card());
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));
        when(scriptService.findVersionById(5L)).thenReturn(Optional.of(version(script)));
        when(scriptService.sanitizeContent("<p>Old version</p>")).thenReturn("<p>Old version</p>");
        when(scriptService.save(script)).thenReturn(script);

        assertEquals("<p>Old version</p>", useCase.execute(1L, 1L, 5L).getContent());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void execute_shouldReturn404WhenVersionBelongsToDifferentScript() {
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(card());
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script(10L)));
        when(scriptService.findVersionById(5L)).thenReturn(Optional.of(version(script(99L))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 1L, 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Versão não encontrada.", ex.getReason());
        verify(scriptService, never()).save(any());
    }

    @Test
    void execute_shouldReturn404WhenVersionNotFound() {
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(card());
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script(10L)));
        when(scriptService.findVersionById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Versão não encontrada.", ex.getReason());
    }

    @Test
    void execute_shouldReturn404WhenThereIsNoScript() {
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(card());
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 1L, 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Script não encontrado para este item.", ex.getReason());
    }

    @Test
    void execute_shouldReturn404ForMissingCardOrOtherTeam() {
        notFound(99L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 99L, 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Card não encontrado.", ex.getReason());
        verifyNoInteractions(scriptService);
    }

    @Test
    void execute_shouldReturn403ForReviewer() {
        reviewer(card(), 3L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 3L, 5L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(FORBIDDEN, ex.getReason());
        verifyNoInteractions(scriptService);
    }
}
