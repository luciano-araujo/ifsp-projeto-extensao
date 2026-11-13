package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.PlanSource;
import br.edu.ifsp.tcc.application.entity.PlanType;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TeamResponse {
    private Long id;
    private String name;
    private TeamRole role;
    private Integer membersCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    /** Plan in force (a finished trial shows as GRATUITO). */
    private PlanType plan;
    private PlanSource planSource;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime planExpiresAt;
}
