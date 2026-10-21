package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseDTO {
    @NotBlank(message = "O titulo do curso e obrigatorio.")
    private String title;

    private String description;
}
