package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScriptExportResponse {
    private Long cardId;
    private String cardTitle;
    private String targetAudience;
    private String pedagogicalObjective;
    private String rawContent;
    private String markdownFormatted;
    private Integer wordCount;
    private Integer estimatedMinutes;
}
