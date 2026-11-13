package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanUsageResponse {
    private Integer members;
    private Integer activeProjects;
    private Integer courses;
    private Long storageBytes;
}
