package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTeamMemberDTO {
    @NotBlank(message = "O e-mail do membro e obrigatorio.")
    @Email(message = "Informe um e-mail valido.")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
    private String email;

    private TeamRole role = TeamRole.CREATOR;
}
