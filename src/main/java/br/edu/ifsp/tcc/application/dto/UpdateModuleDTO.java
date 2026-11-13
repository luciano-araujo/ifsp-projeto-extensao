package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateModuleDTO {
    @Size(max = 255, message = "O título do módulo deve ter no máximo 255 caracteres.")
    private String title;
}
