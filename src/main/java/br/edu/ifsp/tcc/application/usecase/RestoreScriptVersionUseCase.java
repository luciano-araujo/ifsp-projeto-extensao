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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RestoreScriptVersionUseCase {

    private final ScriptService scriptService;
    private final KanbanItemService kanbanItemService;
    private final TeamService teamService;

    public RestoreScriptVersionUseCase(ScriptService scriptService, KanbanItemService kanbanItemService,
                                       TeamService teamService) {
        this.scriptService = scriptService;
        this.kanbanItemService = kanbanItemService;
        this.teamService = teamService;
    }

    @Transactional
    public Script execute(Long kanbanItemId, Long userId, Long versionId) {
        KanbanItem item = kanbanItemService.findCardOfMember(kanbanItemId, userId);
        teamService.requireRole(item.getTeamId(), userId, TeamRole.ADMIN, TeamRole.CREATOR);

        Script script = scriptService.findByKanbanItemId(kanbanItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Script não encontrado para este item."));

        ScriptVersion version = scriptService.findVersionById(versionId)
                .filter(found -> script.getId().equals(found.getScriptId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão não encontrada."));

        script.setContent(version.getContent());
        return scriptService.save(script);
    }
}
