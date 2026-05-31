package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateKanbanItemDTO {
    @NotBlank @Size(min = 3)
    private String title;
    private String description;
}
