package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetScriptUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private GetScriptUseCase useCase;

    @Test
    void execute_shouldReturnScript() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new KanbanItem()));
        Script script = new Script();
        script.setContent("<p>Test</p>");
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));

        Script result = useCase.execute(1L, 1L);
        assertEquals("<p>Test</p>", result.getContent());
    }

    @Test
    void execute_shouldThrowWhenItemNotOwned() {
        when(kanbanItemService.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 99L));
    }

    @Test
    void execute_shouldThrowWhenNoScript() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new KanbanItem()));
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L));
    }
}
