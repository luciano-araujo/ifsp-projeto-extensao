package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.TeamRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PublicInviteResponse {
    private String teamName;
    private TeamRole role;
    private String email;
    private String invitedByName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime expiresAt;
}
