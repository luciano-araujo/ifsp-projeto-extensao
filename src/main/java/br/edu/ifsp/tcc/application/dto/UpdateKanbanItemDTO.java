package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateKanbanItemDTO {
    @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
    private String title;
    @Size(max = 10000, message = "A descrição deve ter no máximo 10000 caracteres.")
    private String description;
    @Size(max = 255, message = "O público-alvo deve ter no máximo 255 caracteres.")
    private String targetAudience;
    @Size(max = 10000, message = "O objetivo pedagógico deve ter no máximo 10000 caracteres.")
    private String pedagogicalObjective;

    /** Due date as yyyy-MM-dd; an empty string clears it and null leaves it unchanged. */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Prazo inválido. Use o formato AAAA-MM-DD.")
    private String dueDate;

    /** Absent leaves the tag unchanged, 0 removes it, any other value must be a tag of the card's team. */
    private Long tagId;
}
