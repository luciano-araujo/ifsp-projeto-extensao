package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Partial edit: only the fields sent are changed. */
@Data
public class UpdateCourseDTO {
    @Size(max = 255, message = "O título do curso deve ter no máximo 255 caracteres.")
    private String title;

    @Size(max = 10000, message = "A descrição deve ter no máximo 10000 caracteres.")
    private String description;
}
