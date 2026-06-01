package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.springframework.stereotype.Component;

@Component
public class RestoreScriptVersionUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;

    public RestoreScriptVersionUseCase(ScriptService scriptService, KanbanItemService kanbanItemService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
    }

    public Script execute(Long kanbanItemId, Long userId, Long versionId) {
        kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));

        Script script = scriptService.findByKanbanItemId(kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Script nao encontrado para este item."));

        ScriptVersion version = scriptService.findVersionById(versionId)
                .orElseThrow(() -> new RuntimeException("Versao nao encontrada."));

        if (!version.getScriptId().equals(script.getId())) {
            throw new RuntimeException("Esta versao nao pertence a este script.");
        }

        script.setContent(version.getContent());
        return scriptService.save(script);
    }
}
