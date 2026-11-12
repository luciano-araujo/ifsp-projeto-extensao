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
class GetScriptVersionsUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private TeamService teamService;
    @InjectMocks private GetScriptVersionsUseCase useCase;

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
    void execute_shouldReturnVersionsToAnyMember() {
        Script script = new Script();
        script.setId(10L);
        when(kanbanItemService.findCardOfMember(1L, 3L)).thenReturn(card());
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));
        when(scriptService.findVersionsByScriptId(10L)).thenReturn(List.of(new ScriptVersion(), new ScriptVersion()));

        assertEquals(2, useCase.execute(1L, 3L).size());
    }

    @Test
    void execute_shouldReturn404WhenThereIsNoScript() {
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(card());
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Script não encontrado para este item.", ex.getReason());
    }

    @Test
    void execute_shouldReturn404ForMissingCardOrOtherTeam() {
        notFound(99L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Card não encontrado.", ex.getReason());
    }
}
