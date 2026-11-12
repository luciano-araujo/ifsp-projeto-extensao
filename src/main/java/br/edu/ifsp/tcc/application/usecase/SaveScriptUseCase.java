package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;

@Component
public class SaveScriptUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;
    private final TeamService teamService;

    public SaveScriptUseCase(ScriptService scriptService, KanbanItemService kanbanItemService,
            TeamService teamService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
        this.teamService = teamService;
    }

    public Script execute(Long kanbanItemId, Long userId, String content) {
        KanbanItem item = kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));
        teamService.requireRole(item.getTeamId(), userId, TeamRole.ADMIN, TeamRole.CREATOR);

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
