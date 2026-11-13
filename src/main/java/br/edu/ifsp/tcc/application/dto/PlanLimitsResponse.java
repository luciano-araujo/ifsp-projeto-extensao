package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Limits of a plan; a null number means unlimited. */
@Data
@AllArgsConstructor
public class PlanLimitsResponse {
    private Integer maxUsers;
    private Integer maxActiveProjects;
    private Integer maxCourses;
    private Boolean automaticSummary;
    private Long storageBytes;
}
