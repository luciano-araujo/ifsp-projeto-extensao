package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateKanbanItemDTO {
    private String title;
    private String description;
    private String targetAudience;
    private String pedagogicalObjective;

    /** Due date as yyyy-MM-dd; an empty string clears it and null leaves it unchanged. */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Prazo invalido. Use o formato AAAA-MM-DD.")
    private String dueDate;
}
