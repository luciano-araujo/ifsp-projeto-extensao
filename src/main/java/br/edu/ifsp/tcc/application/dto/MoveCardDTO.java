package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveCardDTO {
    @NotNull(message = "A coluna de destino e obrigatoria.")
    private Long columnId;

    @PositiveOrZero(message = "A ordem deve ser maior ou igual a zero.")
    private Integer sortOrder;
}
