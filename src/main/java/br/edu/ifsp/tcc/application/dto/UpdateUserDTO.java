package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** PUT /users/{id} accepts only the name; password and e-mail have their own routes (unknown fields are ignored). */
@Data
public class UpdateUserDTO {
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    @Pattern(regexp = "[^\\p{Cntrl}]*", message = "O texto não pode conter quebras de linha ou caracteres de controle.")
    private String name;
}
