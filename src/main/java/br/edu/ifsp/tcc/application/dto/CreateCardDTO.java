package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCardDTO {
    @NotBlank(message = "O titulo e obrigatorio.")
    @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
    private String title;

    @Size(max = 10000, message = "A descrição deve ter no máximo 10000 caracteres.")
    private String description;
    private Long columnId;
    @Size(max = 255, message = "O público-alvo deve ter no máximo 255 caracteres.")
    private String targetAudience;
    @Size(max = 10000, message = "O objetivo pedagógico deve ter no máximo 10000 caracteres.")
    private String pedagogicalObjective;

    @Size(max = 40, message = "A etiqueta deve ter no maximo 40 caracteres.")
    private String tag;

    /** Preferred way to set the tag; when sent, it wins over the tag name. */
    private Long tagId;
}
