package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTagDTO {
    @NotBlank(message = "O nome da etiqueta é obrigatório.")
    @Size(max = 40, message = "O nome da etiqueta deve ter no máximo 40 caracteres.")
    private String name;

    @NotBlank(message = "A cor da etiqueta é obrigatória.")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor inválida. Use o formato #RRGGBB.")
    private String color;
}
