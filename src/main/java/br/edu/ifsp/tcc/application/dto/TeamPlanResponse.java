package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.PlanSource;
import br.edu.ifsp.tcc.application.entity.PlanType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** Plan in force for a team, with its limits and current usage. */
@Data
@AllArgsConstructor
public class TeamPlanResponse {
    private Long teamId;
    private PlanType plan;
    private String planName;
    private PlanSource source;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime expiresAt;

    /** True while the team has not used its one free trial. */
    private Boolean trialAvailable;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime trialUsedAt;

    private PlanLimitsResponse limits;
    private PlanUsageResponse usage;

    /** True when some usage is already above the limit (new creation is blocked until it is back under). */
    private Boolean overLimit;
}
