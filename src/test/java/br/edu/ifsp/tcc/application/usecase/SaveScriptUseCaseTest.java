package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void execute_shouldCreateNewScriptWhenNoneExists() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());
        when(scriptService.save(any(Script.class))).thenAnswer(inv -> inv.getArgument(0));

        Script result = useCase.execute(1L, 1L, "<p>Hello</p>");

        assertEquals("<p>Hello</p>", result.getContent());
        assertEquals(item, result.getKanbanItem());
    }

    @Test
    void execute_shouldUpdateExistingScript() {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        Script existing = new Script();
        existing.setId(1L);
        existing.setKanbanItem(item);
        existing.setContent("<p>Old</p>");

        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(existing));
        when(scriptService.save(existing)).thenReturn(existing);

        Script result = useCase.execute(1L, 1L, "<p>New</p>");

        assertEquals("<p>New</p>", result.getContent());
    }

    @Test
    void execute_shouldThrowWhenItemNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 99L, "content"));
    }

    @Test
    void execute_shouldRejectReviewer() {
        Team team = new Team();
        team.setId(10L);
        KanbanItem item = new KanbanItem();
        item.setTeam(team);
        when(kanbanItemService.findByIdAndUserId(1L, 3L)).thenReturn(Optional.of(item));
        when(teamService.requireRole(10L, 3L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 3L, "x"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(scriptService);
    }
}
