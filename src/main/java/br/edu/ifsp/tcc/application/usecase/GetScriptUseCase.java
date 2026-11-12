package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetScriptUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;

    public GetScriptUseCase(ScriptService scriptService, KanbanItemService kanbanItemService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
    }

    /** The script of the card, or an empty one (not persisted) when nothing was saved yet. */
    @Transactional(readOnly = true)
    public Script execute(Long kanbanItemId, Long userId) {
        KanbanItem item = kanbanItemService.findCardOfMember(kanbanItemId, userId);

        Script script = scriptService.findByKanbanItemId(kanbanItemId)
                .orElseGet(() -> scriptService.emptyScript(item));
        return scriptService.sanitizeForRead(script);
    }
}
