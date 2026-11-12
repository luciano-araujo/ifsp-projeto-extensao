package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class GetScriptVersionsUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;

    public GetScriptVersionsUseCase(ScriptService scriptService, KanbanItemService kanbanItemService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
    }

    public List<ScriptVersion> execute(Long kanbanItemId, Long userId) {
        kanbanItemService.findCardOfMember(kanbanItemId, userId);

        var script = scriptService.findByKanbanItemId(kanbanItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Script não encontrado para este item."));

        return scriptService.findVersionsByScriptId(script.getId());
    }
}
