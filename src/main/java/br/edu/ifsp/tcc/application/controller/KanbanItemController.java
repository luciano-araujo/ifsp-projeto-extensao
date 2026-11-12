package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.UpdateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/kanban")
public class KanbanItemController {

    private final GetKanbanItemByIdUseCase getKanbanItemByIdUseCase;
    private final UpdateKanbanItemUseCase updateKanbanItemUseCase;
    private final DeleteKanbanItemUseCase deleteKanbanItemUseCase;

    public KanbanItemController(GetKanbanItemByIdUseCase getKanbanItemByIdUseCase,
                                UpdateKanbanItemUseCase updateKanbanItemUseCase,
                                DeleteKanbanItemUseCase deleteKanbanItemUseCase) {
        this.getKanbanItemByIdUseCase = getKanbanItemByIdUseCase;
        this.updateKanbanItemUseCase = updateKanbanItemUseCase;
        this.deleteKanbanItemUseCase = deleteKanbanItemUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<KanbanItem> getById(@AuthenticationPrincipal User user,
                                               @PathVariable Long id) {
        return getKanbanItemByIdUseCase.execute(id, user.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<KanbanItem> update(@AuthenticationPrincipal User user,
                                              @PathVariable Long id,
                                              @Valid @RequestBody UpdateKanbanItemDTO dto) {
        KanbanItem item = updateKanbanItemUseCase.execute(id, user.getId(), dto);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                        @PathVariable Long id) {
        deleteKanbanItemUseCase.execute(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
