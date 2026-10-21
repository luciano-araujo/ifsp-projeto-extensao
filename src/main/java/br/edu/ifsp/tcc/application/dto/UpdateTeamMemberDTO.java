package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateTeamMemberDTO {
    @NotNull(message = "O papel do membro e obrigatorio.")
    private TeamRole role;
}
