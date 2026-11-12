package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskDTO {
    @NotBlank(message = "O titulo da tarefa e obrigatorio.")
    @Size(max = 255, message = "O título da tarefa deve ter no máximo 255 caracteres.")
    private String title;
}
