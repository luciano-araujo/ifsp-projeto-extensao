package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetScriptVersionsUseCaseTest {

    @Mock private ScriptService scriptService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private GetScriptVersionsUseCase useCase;

    @Test
    void execute_shouldReturnVersionsList() {
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(new KanbanItem()));
        Script script = new Script();
        script.setId(10L);
        when(scriptService.findByKanbanItemId(1L)).thenReturn(Optional.of(script));
        when(scriptService.findVersionsByScriptId(10L)).thenReturn(List.of(new ScriptVersion(), new ScriptVersion()));

        List<ScriptVersion> result = useCase.execute(1L, 1L);

        assertEquals(2, result.size());
    }

    @Test
    void execute_shouldThrowWhenItemNotOwned() {
        when(kanbanItemService.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 99L));
    }
}
