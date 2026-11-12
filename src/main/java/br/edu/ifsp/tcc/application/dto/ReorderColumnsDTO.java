package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderColumnsDTO {
    @NotNull(message = "A lista de colunas é obrigatória.")
    private List<@NotNull(message = "A lista de colunas não pode ter itens vazios.") Long> columnIds;
}
