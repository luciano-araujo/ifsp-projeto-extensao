package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.PlanType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartTrialDTO {
    @NotNull(message = "Informe o plano do teste (SOLO, PRO ou AGENCIA).")
    private PlanType plan;
}
