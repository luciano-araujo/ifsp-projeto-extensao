package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.TeamRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTeamMemberDTO {
    @NotBlank(message = "O e-mail do membro e obrigatorio.")
    @Email(message = "Informe um e-mail valido.")
    private String email;

    private TeamRole role = TeamRole.CREATOR;
}
