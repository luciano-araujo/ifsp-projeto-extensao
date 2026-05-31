package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.ConvertToProjectDTO;
import br.edu.ifsp.tcc.application.dto.CreateKanbanItemDTO;
import br.edu.ifsp.tcc.application.dto.UpdateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kanban")
public class KanbanItemController {

    private final CreateKanbanItemUseCase createKanbanItemUseCase;
    private final GetKanbanItemsByUserUseCase getKanbanItemsByUserUseCase;
    private final UpdateKanbanItemUseCase updateKanbanItemUseCase;
    private final ConvertToProjectUseCase convertToProjectUseCase;
    private final DeleteKanbanItemUseCase deleteKanbanItemUseCase;

    public KanbanItemController(CreateKanbanItemUseCase createKanbanItemUseCase,
                                GetKanbanItemsByUserUseCase getKanbanItemsByUserUseCase,
                                UpdateKanbanItemUseCase updateKanbanItemUseCase,
                                ConvertToProjectUseCase convertToProjectUseCase,
                                DeleteKanbanItemUseCase deleteKanbanItemUseCase) {
        this.createKanbanItemUseCase = createKanbanItemUseCase;
        this.getKanbanItemsByUserUseCase = getKanbanItemsByUserUseCase;
        this.updateKanbanItemUseCase = updateKanbanItemUseCase;
        this.convertToProjectUseCase = convertToProjectUseCase;
        this.deleteKanbanItemUseCase = deleteKanbanItemUseCase;
    }

    @PostMapping
    public ResponseEntity<KanbanItem> create(@AuthenticationPrincipal User user,
                                              @Valid @RequestBody CreateKanbanItemDTO dto) {
        KanbanItem item = createKanbanItemUseCase.execute(dto, user);
        return ResponseEntity.ok(item);
    }

    @GetMapping
    public ResponseEntity<List<KanbanItem>> list(@AuthenticationPrincipal User user,
                                                  @RequestParam(required = false) KanbanItemState state) {
        List<KanbanItem> items = getKanbanItemsByUserUseCase.execute(user.getId(), state);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<KanbanItem> getById(@AuthenticationPrincipal User user,
                                               @PathVariable Long id) {
        return getKanbanItemsByUserUseCase.execute(user.getId(), null).stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
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

    @PostMapping("/{id}/convert")
    public ResponseEntity<KanbanItem> convert(@AuthenticationPrincipal User user,
                                               @PathVariable Long id,
                                               @Valid @RequestBody ConvertToProjectDTO dto) {
        KanbanItem item = convertToProjectUseCase.execute(id, user.getId(), dto);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                        @PathVariable Long id) {
        deleteKanbanItemUseCase.execute(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
