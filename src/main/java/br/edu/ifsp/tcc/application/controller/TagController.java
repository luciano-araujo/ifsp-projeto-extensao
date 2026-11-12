package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.CreateTagDTO;
import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.dto.UpdateTagDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.CreateTagUseCase;
import br.edu.ifsp.tcc.application.usecase.DeleteTagUseCase;
import br.edu.ifsp.tcc.application.usecase.ListTagsUseCase;
import br.edu.ifsp.tcc.application.usecase.UpdateTagUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/tags")
public class TagController {

    private final ListTagsUseCase listTagsUseCase;
    private final CreateTagUseCase createTagUseCase;
    private final UpdateTagUseCase updateTagUseCase;
    private final DeleteTagUseCase deleteTagUseCase;

    public TagController(ListTagsUseCase listTagsUseCase,
                         CreateTagUseCase createTagUseCase,
                         UpdateTagUseCase updateTagUseCase,
                         DeleteTagUseCase deleteTagUseCase) {
        this.listTagsUseCase = listTagsUseCase;
        this.createTagUseCase = createTagUseCase;
        this.updateTagUseCase = updateTagUseCase;
        this.deleteTagUseCase = deleteTagUseCase;
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> list(@AuthenticationPrincipal User user, @PathVariable Long teamId) {
        return ResponseEntity.ok(listTagsUseCase.execute(teamId, user.getId()));
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@AuthenticationPrincipal User user,
                                              @PathVariable Long teamId,
                                              @Valid @RequestBody CreateTagDTO dto) {
        return ResponseEntity.ok(createTagUseCase.execute(teamId, user.getId(), dto));
    }

    @PatchMapping("/{tagId}")
    public ResponseEntity<TagResponse> update(@AuthenticationPrincipal User user,
                                              @PathVariable Long teamId,
                                              @PathVariable Long tagId,
                                              @Valid @RequestBody UpdateTagDTO dto) {
        return ResponseEntity.ok(updateTagUseCase.execute(teamId, user.getId(), tagId, dto));
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable Long teamId,
                                       @PathVariable Long tagId) {
        deleteTagUseCase.execute(teamId, user.getId(), tagId);
        return ResponseEntity.noContent().build();
    }
}
