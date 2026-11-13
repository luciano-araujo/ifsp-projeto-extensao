package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.PlanType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PlanResponse {
    private PlanType plan;
    private String name;
    private BigDecimal monthlyPrice;
    private PlanLimitsResponse limits;
}
