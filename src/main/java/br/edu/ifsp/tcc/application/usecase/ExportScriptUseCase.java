package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ScriptExportResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.springframework.stereotype.Component;

@Component
public class ExportScriptUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;

    public ExportScriptUseCase(ScriptService scriptService, KanbanItemService kanbanItemService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
    }

    /** Any member of the card's team (reviewers included) can export; a card without script exports empty. */
    public ScriptExportResponse execute(Long kanbanItemId, Long userId) {
        KanbanItem item = kanbanItemService.findCardOfMember(kanbanItemId, userId);

        String content = scriptService.findByKanbanItemId(kanbanItemId)
                .map(script -> script.getContent())
                .orElse("");
        return scriptService.toExportResponse(item, scriptService.sanitizeContent(content));
    }
}
