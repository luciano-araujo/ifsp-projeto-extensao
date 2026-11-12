package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class GetScriptVersionsUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;

    public GetScriptVersionsUseCase(ScriptService scriptService, KanbanItemService kanbanItemService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
    }

    /** Versions of the card script; empty list when no script was saved yet. */
    @Transactional(readOnly = true)
    public List<ScriptVersion> execute(Long kanbanItemId, Long userId) {
        kanbanItemService.findCardOfMember(kanbanItemId, userId);

        return scriptService.findByKanbanItemId(kanbanItemId)
                .map(script -> scriptService.sanitizeForRead(scriptService.findVersionsByScriptId(script.getId())))
                .orElseGet(List::of);
    }
}
