package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.MaterialDownload;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.DeleteMaterialUseCase;
import br.edu.ifsp.tcc.application.usecase.DownloadMaterialUseCase;
import br.edu.ifsp.tcc.application.usecase.ListCardMaterialsUseCase;
import br.edu.ifsp.tcc.application.usecase.UploadMaterialUseCase;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}")
public class MaterialController {

    private final UploadMaterialUseCase uploadMaterialUseCase;
    private final ListCardMaterialsUseCase listCardMaterialsUseCase;
    private final DownloadMaterialUseCase downloadMaterialUseCase;
    private final DeleteMaterialUseCase deleteMaterialUseCase;

    public MaterialController(UploadMaterialUseCase uploadMaterialUseCase,
                              ListCardMaterialsUseCase listCardMaterialsUseCase,
                              DownloadMaterialUseCase downloadMaterialUseCase,
                              DeleteMaterialUseCase deleteMaterialUseCase) {
        this.uploadMaterialUseCase = uploadMaterialUseCase;
        this.listCardMaterialsUseCase = listCardMaterialsUseCase;
        this.downloadMaterialUseCase = downloadMaterialUseCase;
        this.deleteMaterialUseCase = deleteMaterialUseCase;
    }

    @PostMapping(value = "/cards/{cardId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> upload(@AuthenticationPrincipal User user,
                                                   @PathVariable Long teamId,
                                                   @PathVariable Long cardId,
                                                   @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(uploadMaterialUseCase.execute(teamId, user.getId(), cardId, file));
    }

    @GetMapping("/cards/{cardId}/materials")
    public ResponseEntity<List<MaterialResponse>> list(@AuthenticationPrincipal User user,
                                                       @PathVariable Long teamId,
                                                       @PathVariable Long cardId) {
        return ResponseEntity.ok(listCardMaterialsUseCase.execute(teamId, user.getId(), cardId));
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long materialId,
                                             @RequestParam(defaultValue = "false") boolean inline) {
        MaterialDownload file = downloadMaterialUseCase.execute(teamId, user.getId(), materialId, inline);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, file.getContentDisposition())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.getResource());
    }

    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user,
                                       @PathVariable Long teamId,
                                       @PathVariable Long materialId) {
        deleteMaterialUseCase.execute(teamId, user.getId(), materialId);
        return ResponseEntity.noContent().build();
    }
}
