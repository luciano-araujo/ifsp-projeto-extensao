package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserDTO {
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    private String name;
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
    private String email;
    private String password;
}
