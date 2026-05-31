package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.MessageResponse;
import br.edu.ifsp.tcc.application.dto.SaveScriptDTO;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kanban/{kanbanItemId}/script")
public class ScriptController {

    private final SaveScriptUseCase saveScriptUseCase;
    private final GetScriptUseCase getScriptUseCase;
    private final CreateScriptVersionUseCase createScriptVersionUseCase;
    private final GetScriptVersionsUseCase getScriptVersionsUseCase;
    private final RestoreScriptVersionUseCase restoreScriptVersionUseCase;

    public ScriptController(SaveScriptUseCase saveScriptUseCase,
                            GetScriptUseCase getScriptUseCase,
                            CreateScriptVersionUseCase createScriptVersionUseCase,
                            GetScriptVersionsUseCase getScriptVersionsUseCase,
                            RestoreScriptVersionUseCase restoreScriptVersionUseCase) {
        this.saveScriptUseCase = saveScriptUseCase;
        this.getScriptUseCase = getScriptUseCase;
        this.createScriptVersionUseCase = createScriptVersionUseCase;
        this.getScriptVersionsUseCase = getScriptVersionsUseCase;
        this.restoreScriptVersionUseCase = restoreScriptVersionUseCase;
    }

    @PutMapping
    public ResponseEntity<Script> save(@AuthenticationPrincipal User user,
                                        @PathVariable Long kanbanItemId,
                                        @RequestBody SaveScriptDTO dto) {
        Script script = saveScriptUseCase.execute(kanbanItemId, user.getId(), dto.getContent());
        return ResponseEntity.ok(script);
    }

    @GetMapping
    public ResponseEntity<Script> get(@AuthenticationPrincipal User user,
                                       @PathVariable Long kanbanItemId) {
        Script script = getScriptUseCase.execute(kanbanItemId, user.getId());
        return ResponseEntity.ok(script);
    }

    @PostMapping("/versions")
    public ResponseEntity<ScriptVersion> createVersion(@AuthenticationPrincipal User user,
                                                        @PathVariable Long kanbanItemId) {
        ScriptVersion version = createScriptVersionUseCase.execute(kanbanItemId, user.getId());
        return ResponseEntity.ok(version);
    }

    @GetMapping("/versions")
    public ResponseEntity<List<ScriptVersion>> listVersions(@AuthenticationPrincipal User user,
                                                             @PathVariable Long kanbanItemId) {
        List<ScriptVersion> versions = getScriptVersionsUseCase.execute(kanbanItemId, user.getId());
        return ResponseEntity.ok(versions);
    }

    @PostMapping("/versions/{versionId}/restore")
    public ResponseEntity<Script> restoreVersion(@AuthenticationPrincipal User user,
                                                  @PathVariable Long kanbanItemId,
                                                  @PathVariable Long versionId) {
        Script script = restoreScriptVersionUseCase.execute(kanbanItemId, user.getId(), versionId);
        return ResponseEntity.ok(script);
    }
}
