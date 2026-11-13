package br.edu.ifsp.tcc.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private String uuid;
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
    private String tag;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Integer votes;
    private Boolean votedByMe;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;

    private Long tagId;
    private String tagColor;

    /** When the card entered its current column; null for ideas. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime columnSince;

    /** Number of attached materials (always 0 for ideas). */
    private Integer materialsCount;
    /** NONE until a reviewer decides; then the decision of the latest review. Ideas are always NONE. */
    private ReviewStatus reviewStatus;

    /** YouTube link of the published video; null when none (always null for ideas). */
    private String videoUrl;
}
