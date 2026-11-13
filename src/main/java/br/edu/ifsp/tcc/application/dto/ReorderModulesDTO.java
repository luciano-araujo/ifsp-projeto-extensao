package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderModulesDTO {
    @NotNull(message = "A lista de módulos é obrigatória.")
    private List<@NotNull(message = "A lista de módulos não pode ter itens vazios.") Long> moduleIds;
}
