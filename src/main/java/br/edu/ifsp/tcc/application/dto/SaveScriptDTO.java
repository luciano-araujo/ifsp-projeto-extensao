package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveScriptDTO {
    @Size(max = 500_000, message = "O roteiro deve ter no máximo 500.000 caracteres.")
    private String content;
}
