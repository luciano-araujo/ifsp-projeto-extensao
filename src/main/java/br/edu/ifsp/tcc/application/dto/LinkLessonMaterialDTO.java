package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LinkLessonMaterialDTO {

    @NotNull(message = "Informe o material.")
    private Long materialId;
}
