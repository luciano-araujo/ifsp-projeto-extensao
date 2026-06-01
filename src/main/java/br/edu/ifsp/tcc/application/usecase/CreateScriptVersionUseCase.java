package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import org.springframework.stereotype.Component;

@Component
public class CreateScriptVersionUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;

    public CreateScriptVersionUseCase(ScriptService scriptService, KanbanItemService kanbanItemService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
    }

    public ScriptVersion execute(Long kanbanItemId, Long userId) {
        kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));

        Script script = scriptService.findByKanbanItemId(kanbanItemId)
                .orElseThrow(() -> new RuntimeException("Script nao encontrado. Salve o conteudo antes de criar uma versao."));

        ScriptVersion version = new ScriptVersion();
        version.setScript(script);
        version.setContent(script.getContent());
        return scriptService.saveVersion(version);
    }
}
