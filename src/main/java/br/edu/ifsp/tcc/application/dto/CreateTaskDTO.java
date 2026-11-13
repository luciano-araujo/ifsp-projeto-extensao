package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskDTO {
    @NotBlank(message = "O título da tarefa é obrigatório.")
    @Size(max = 255, message = "O título da tarefa deve ter no máximo 255 caracteres.")
    @Pattern(regexp = "[^\\p{Cntrl}]*", message = "O texto não pode conter quebras de linha ou caracteres de controle.")
    private String title;
}
