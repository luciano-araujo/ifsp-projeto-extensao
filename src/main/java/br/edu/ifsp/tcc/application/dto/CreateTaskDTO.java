package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskDTO {
    @NotBlank(message = "O titulo da tarefa e obrigatorio.")
    private String title;
}
