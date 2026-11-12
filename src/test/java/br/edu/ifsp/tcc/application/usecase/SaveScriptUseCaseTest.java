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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveScriptUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private TeamService teamService;
    @InjectMocks private SaveScriptUseCase useCase;

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

    @Test
    void execute_shouldCreateNewScriptWhenNoneExists() {
        KanbanItem item = card();
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());
        when(scriptService.save(any(Script.class))).thenAnswer(inv -> inv.getArgument(0));

        Script result = useCase.execute(1L, 1L, "<p>Hello</p>");

        assertEquals("<p>Hello</p>", result.getContent());
        assertSame(item, result.getKanbanItem());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void execute_shouldUpdateExistingScript() {
        Script existing = new Script();
        existing.setId(1L);
        existing.setContent("<p>Old</p>");
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(card());
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(existing));
        when(scriptService.save(existing)).thenReturn(existing);

        assertEquals("<p>New</p>", useCase.execute(1L, 1L, "<p>New</p>").getContent());
    }

    @Test
    void execute_shouldReturn404ForMissingCardOrOtherTeam() {
        notFound(99L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 99L, "x"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Card não encontrado.", ex.getReason());
        verifyNoInteractions(scriptService);
    }

    @Test
    void execute_shouldReturn403ForReviewer() {
        reviewer(card(), 3L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 3L, "x"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(FORBIDDEN, ex.getReason());
        verifyNoInteractions(scriptService);
    }

    @Test
    void execute_shouldStoreEmptyStringWhenContentIsNull() {
        KanbanItem item = card();
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());
        when(scriptService.save(any(Script.class))).thenAnswer(inv -> inv.getArgument(0));

        Script result = useCase.execute(1L, 1L, null);

        assertEquals("", result.getContent());
    }
}
