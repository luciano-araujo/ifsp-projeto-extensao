package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestoreScriptVersionUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private TeamService teamService;
    @InjectMocks private RestoreScriptVersionUseCase useCase;

    @Test
    void execute_shouldRestoreContentFromVersion() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new KanbanItem()));

        Script script = new Script();
        script.setId(10L);
        script.setContent("<p>Current</p>");
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));

        ScriptVersion version = new ScriptVersion();
        version.setId(5L);
        version.setScript(script);
        version.setContent("<p>Old version</p>");
        when(scriptService.findVersionById(5L)).thenReturn(Optional.of(version));
        when(scriptService.save(script)).thenReturn(script);

        Script result = useCase.execute(1L, 1L, 5L);

        assertEquals("<p>Old version</p>", result.getContent());
    }

    @Test
    void execute_shouldThrowWhenVersionBelongsToDifferentScript() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new KanbanItem()));

        Script script = new Script();
        script.setId(10L);
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));

        Script otherScript = new Script();
        otherScript.setId(99L);
        ScriptVersion version = new ScriptVersion();
        version.setId(5L);
        version.setScript(otherScript);
        when(scriptService.findVersionById(5L)).thenReturn(Optional.of(version));

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L, 5L));
    }

    @Test
    void execute_shouldThrowWhenVersionNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new KanbanItem()));
        Script script = new Script();
        script.setId(10L);
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));
        when(scriptService.findVersionById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L, 99L));
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

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 3L, 5L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(scriptService);
    }
}
