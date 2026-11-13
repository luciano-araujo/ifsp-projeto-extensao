package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterTokenRequest {
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
    @Pattern(regexp = "[^\\p{Cntrl}]*", message = "O texto não pode conter quebras de linha ou caracteres de controle.")
    private String name;
    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
    private String email;
}
