package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {
    @NotBlank(message = "A senha atual é obrigatória.")
    @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
    private String currentPassword;

    @NotBlank(message = "A nova senha é obrigatória.")
    @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
    @Size(max = 72, message = "A senha deve ter no máximo 72 caracteres.")
    private String newPassword;
}
