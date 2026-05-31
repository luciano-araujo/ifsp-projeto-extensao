package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmRegistrationRequest {
    @NotBlank @Email
    private String email;
    @NotBlank @Size(min = 6, max = 6)
    private String token;
    @NotBlank @Size(min = 6)
    private String password;
}
