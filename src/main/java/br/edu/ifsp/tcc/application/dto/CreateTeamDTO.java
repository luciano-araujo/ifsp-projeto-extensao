package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTeamDTO {
    @NotBlank(message = "O nome da equipe é obrigatório.")
    @Size(max = 120, message = "O nome da equipe deve ter no máximo 120 caracteres.")
    private String name;
}
