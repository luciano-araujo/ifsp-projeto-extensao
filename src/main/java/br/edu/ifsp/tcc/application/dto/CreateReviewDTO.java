package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReviewDTO {
    @NotNull(message = "Informe a decisão da revisão.")
    private ReviewDecision decision;

    /** Required (non blank) when the decision is CHANGES_REQUESTED. */
    @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres.")
    private String comment;
}
