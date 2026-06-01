package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.springframework.stereotype.Component;

@Component
public class SaveScriptUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;

    public SaveScriptUseCase(ScriptService scriptService, KanbanItemService kanbanItemService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
    }

    public Script execute(Long kanbanItemId, Long userId, String content) {
        KanbanItem item = kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));

        Script script = scriptService.findByKanbanItemId(kanbanItemId)
                .orElseGet(() -> {
                    Script newScript = new Script();
                    newScript.setKanbanItem(item);
                    return newScript;
                });

        script.setContent(content);
        return scriptService.save(script);
    }
}
