package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.PlanType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivatePlanDTO {
    @NotNull(message = "Informe o plano (GRATUITO, SOLO, PRO ou AGENCIA).")
    private PlanType plan;

    /** Optional end of the activation (UTC); without it the plan stays until changed. Ignored for GRATUITO. */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime expiresAt;
}
