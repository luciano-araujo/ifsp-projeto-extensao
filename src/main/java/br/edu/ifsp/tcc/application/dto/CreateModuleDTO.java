package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateModuleDTO {
    @NotBlank
    @Size(max = 255, message = "O título do módulo deve ter no máximo 255 caracteres.")
    private String title;
}
