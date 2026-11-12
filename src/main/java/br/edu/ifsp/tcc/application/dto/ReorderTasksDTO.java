package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderTasksDTO {
    @NotNull(message = "A lista de tarefas é obrigatória.")
    private List<@NotNull(message = "A lista de tarefas não pode ter itens vazios.") Long> taskIds;
}
