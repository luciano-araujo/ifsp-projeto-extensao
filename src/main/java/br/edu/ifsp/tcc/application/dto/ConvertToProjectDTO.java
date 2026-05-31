package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConvertToProjectDTO {
    @NotBlank @Size(min = 3)
    private String title;
    @NotBlank @Size(min = 3)
    private String targetAudience;
    @NotBlank @Size(min = 10)
    private String pedagogicalObjective;
}
