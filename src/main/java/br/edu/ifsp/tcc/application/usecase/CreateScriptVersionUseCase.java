package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CreateScriptVersionUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;
    private final TeamService teamService;

    public CreateScriptVersionUseCase(ScriptService scriptService, KanbanItemService kanbanItemService,
                                      TeamService teamService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
        this.teamService = teamService;
    }

    public ScriptVersion execute(Long kanbanItemId, Long userId) {
        KanbanItem item = kanbanItemService.findCardOfMember(kanbanItemId, userId);
        teamService.requireRole(item.getTeamId(), userId, TeamRole.ADMIN, TeamRole.CREATOR);

        Script script = scriptService.findByKanbanItemId(kanbanItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Script não encontrado. Salve o conteúdo antes de criar uma versão."));

        ScriptVersion version = new ScriptVersion();
        version.setScript(script);
        version.setContent(script.getContent());
        return scriptService.saveVersion(version);
    }
}
