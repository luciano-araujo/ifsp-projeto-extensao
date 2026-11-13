package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.PlanResponse;
import br.edu.ifsp.tcc.application.service.PlanService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListPlansUseCase {

    private final PlanService planService;

    public ListPlansUseCase(PlanService planService) {
        this.planService = planService;
    }

    /** The catalogue of plans with their prices and limits (a null limit means unlimited). */
    public List<PlanResponse> execute() {
        return planService.listPlans();
    }
}
