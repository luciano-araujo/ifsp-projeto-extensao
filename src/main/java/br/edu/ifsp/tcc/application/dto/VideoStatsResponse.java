package br.edu.ifsp.tcc.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class VideoStatsResponse {
    private String videoId;
    private String videoUrl;

    /** Title and publication date come from the latest snapshot; null while there is none. */
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime publishedAt;

    private VideoLatestResponse latest;

    /** Oldest first, at most the last 60 snapshots. */
    private List<VideoHistoryPointResponse> history;
}
