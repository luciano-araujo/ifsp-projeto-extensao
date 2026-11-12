package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Both fields are optional; a field that is not sent stays as it is. */
@Data
public class UpdateTagDTO {
    @Size(max = 40, message = "O nome da etiqueta deve ter no máximo 40 caracteres.")
    private String name;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor inválida. Use o formato #RRGGBB.")
    private String color;
}
