package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterTokenRequest {
    @NotBlank
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    private String name;
    @NotBlank @Email
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
    private String email;
}
