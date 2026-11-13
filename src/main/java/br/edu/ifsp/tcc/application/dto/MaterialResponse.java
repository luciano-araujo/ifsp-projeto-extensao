package br.edu.ifsp.tcc.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MaterialResponse {
    private Long id;
    private String uuid;
    private Long cardId;
    private String fileName;
    private String contentType;
    private Long sizeBytes;

    /** Null after the uploader deletes the account. */
    private MaterialUploaderResponse uploadedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    private String downloadUrl;
}
