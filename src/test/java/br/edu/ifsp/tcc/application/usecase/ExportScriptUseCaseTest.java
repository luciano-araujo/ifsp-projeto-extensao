package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ScriptExportResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
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
class ExportScriptUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private ExportScriptUseCase useCase;

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

    @Test
    void execute_shouldExportTheSavedScriptForAnyMember() {
        KanbanItem item = card();
        Script script = new Script();
        script.setContent("<p>Ola</p><script>x</script>");
        ScriptExportResponse response = new ScriptExportResponse(1L, "T", "A", "O", "<p>Ola</p>", "Ola", 1, 1);
        when(kanbanItemService.findCardOfMember(1L, 3L)).thenReturn(item);
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));
        when(scriptService.sanitizeContent("<p>Ola</p><script>x</script>")).thenReturn("<p>Ola</p>");
        when(scriptService.toExportResponse(item, "<p>Ola</p>")).thenReturn(response);

        assertSame(response, useCase.execute(1L, 3L));
    }

    @Test
    void execute_shouldExportEmptyContentWhenThereIsNoScript() {
        KanbanItem item = card();
        ScriptExportResponse response = new ScriptExportResponse(1L, "T", null, null, "", "", 0, 1);
        when(kanbanItemService.findCardOfMember(1L, 3L)).thenReturn(item);
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());
        when(scriptService.sanitizeContent("")).thenReturn("");
        when(scriptService.toExportResponse(item, "")).thenReturn(response);

        assertSame(response, useCase.execute(1L, 3L));
    }

    @Test
    void execute_shouldReturn404ForMissingCardOrOtherTeam() {
        notFound(99L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Card não encontrado.", ex.getReason());
        verifyNoInteractions(scriptService);
    }
}
