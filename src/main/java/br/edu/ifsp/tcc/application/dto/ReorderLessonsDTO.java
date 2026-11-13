package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderLessonsDTO {
    @NotNull(message = "A lista de aulas é obrigatória.")
    private List<@NotNull(message = "A lista de aulas não pode ter itens vazios.") Long> lessonIds;
}
