package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateKanbanItemDTO {
    private String title;
    private String description;
    private KanbanItemState state;
    private String targetAudience;
    private String pedagogicalObjective;
    @Min(0) @Max(100)
    private Integer progress;

    /** Due date as yyyy-MM-dd; an empty string clears it and null leaves it unchanged. */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Prazo invalido. Use o formato AAAA-MM-DD.")
    private String dueDate;
}
