package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
}
