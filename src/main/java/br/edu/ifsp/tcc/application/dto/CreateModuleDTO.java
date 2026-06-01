package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateModuleDTO {
    @NotBlank
    private String title;
}
