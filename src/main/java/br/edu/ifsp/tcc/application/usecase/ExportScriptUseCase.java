package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ScriptExportResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;

@Component
public class ExportScriptUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;
    private final TeamService teamService;

    public ExportScriptUseCase(ScriptService scriptService, KanbanItemService kanbanItemService,
                               TeamService teamService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
        this.teamService = teamService;
    }

    /** Any member of the card's team (reviewers included) can export; a card without script exports empty. */
    public ScriptExportResponse execute(Long kanbanItemId, Long userId) {
        KanbanItem item = kanbanItemService.findByIdAndUserId(kanbanItemId, userId)
                .orElseThrow(() -> new RuntimeException("Item nao encontrado."));
        teamService.requireMember(item.getTeamId(), userId);

        String content = scriptService.findByKanbanItemId(kanbanItemId)
                .map(script -> script.getContent())
                .orElse("");
        return scriptService.toExportResponse(item, content);
    }
}
