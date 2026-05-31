package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateScriptVersionUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private CreateScriptVersionUseCase useCase;

    @Test
    void execute_shouldCreateVersionFromCurrentContent() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new KanbanItem()));
        Script script = new Script();
        script.setId(1L);
        script.setContent("<p>Current content</p>");
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));
        when(scriptService.saveVersion(any())).thenAnswer(inv -> inv.getArgument(0));

        ScriptVersion result = useCase.execute(1L, 1L);

        assertEquals("<p>Current content</p>", result.getContent());
        assertEquals(script, result.getScript());
    }

    @Test
    void execute_shouldThrowWhenNoScript() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new KanbanItem()));
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 1L));
    }
}
