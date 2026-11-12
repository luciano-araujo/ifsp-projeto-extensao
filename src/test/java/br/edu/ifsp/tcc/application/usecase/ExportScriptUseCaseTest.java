package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ScriptExportResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.Team;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportScriptUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private TeamService teamService;
    @InjectMocks private ExportScriptUseCase useCase;

    private KanbanItem card() {
        Team team = new Team();
        team.setId(10L);
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTeam(team);
        return item;
    }

    @Test
    void execute_shouldExportTheSavedScriptForAnyMember() {
        KanbanItem item = card();
        Script script = new Script();
        script.setContent("<p>Ola</p>");
        ScriptExportResponse response = new ScriptExportResponse(1L, "T", "A", "O", "<p>Ola</p>", "Ola", 1, 1);
        when(kanbanItemService.findByIdAndUserId(1L, 3L)).thenReturn(Optional.of(item));
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));
        when(scriptService.toExportResponse(item, "<p>Ola</p>")).thenReturn(response);

        assertSame(response, useCase.execute(1L, 3L));

        verify(teamService).requireMember(10L, 3L);
    }

    @Test
    void execute_shouldExportEmptyContentWhenThereIsNoScript() {
        KanbanItem item = card();
        ScriptExportResponse response = new ScriptExportResponse(1L, "T", null, null, "", "", 0, 1);
        when(kanbanItemService.findByIdAndUserId(1L, 3L)).thenReturn(Optional.of(item));
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());
        when(scriptService.toExportResponse(item, "")).thenReturn(response);

        assertSame(response, useCase.execute(1L, 3L));
    }

    @Test
    void execute_shouldThrowWhenCardNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 99L));
        verifyNoInteractions(teamService, scriptService);
    }

    @Test
    void execute_shouldRejectNonMember() {
        KanbanItem item = card();
        when(kanbanItemService.findByIdAndUserId(1L, 9L)).thenReturn(Optional.of(item));
        when(teamService.requireMember(10L, 9L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não faz parte desta equipe."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 9L));
        verifyNoInteractions(scriptService);
    }
}
