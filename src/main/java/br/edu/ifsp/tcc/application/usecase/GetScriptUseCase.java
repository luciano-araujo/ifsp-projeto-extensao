package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.springframework.stereotype.Component;

@Component
public class GetScriptUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;

    public GetScriptUseCase(ScriptService scriptService, KanbanItemService kanbanItemService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
    }

    public Script execute(Long kanbanItemId, Long userId) {
        kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));

        return scriptService.findByKanbanItemId(kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Script nao encontrado para este item."));
    }
}
