package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmEmailChangeDTO {
    @NotBlank(message = "O código de verificação é obrigatório.")
    @Size(min = 6, max = 6, message = "O código de verificação deve ter 6 caracteres.")
    private String token;
}
