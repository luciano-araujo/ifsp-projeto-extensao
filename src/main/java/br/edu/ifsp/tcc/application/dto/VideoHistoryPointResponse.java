package br.edu.ifsp.tcc.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class VideoHistoryPointResponse {
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime capturedAt;

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
}
