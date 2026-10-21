package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCardDTO {
    @NotBlank(message = "O titulo e obrigatorio.")
    private String title;

    private String description;
    private Long columnId;
    private String targetAudience;
    private String pedagogicalObjective;
}
