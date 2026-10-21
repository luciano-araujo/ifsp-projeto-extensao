package br.edu.ifsp.tcc.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private Long teamId;
    private String title;
    private String description;
    private String type;
    private Long columnId;
    private Integer sortOrder;
    private Integer progress;
    private Integer tasksTotal;
    private Integer tasksDone;
    private String targetAudience;
    private String pedagogicalObjective;
    private List<CardTaskResponse> tasks;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;
}
