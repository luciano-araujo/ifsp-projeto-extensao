package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.TeamRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/** Response bodies of the team, board and task routes (shape expected by the frontend store). */
public final class TeamResponses {

    private TeamResponses() {}

    private static final String ISO = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public record TeamResponse(
            Long id,
            String name,
            TeamRole role,
            int membersCount,
            @JsonFormat(pattern = ISO) LocalDateTime createdAt) {}

    public record TeamMemberResponse(
            Long id,
            Long userId,
            String name,
            String email,
            TeamRole role,
            @JsonFormat(pattern = ISO) LocalDateTime joinedAt) {}

    public record CardTaskResponse(
            Long id,
            Long cardId,
            String title,
            boolean completed,
            int sortOrder,
            @JsonFormat(pattern = ISO) LocalDateTime createdAt) {}

    public record CardResponse(
            Long id,
            Long teamId,
            String title,
            String description,
            String type,
            Long columnId,
            int sortOrder,
            int progress,
            int tasksTotal,
            int tasksDone,
            String targetAudience,
            String pedagogicalObjective,
            List<CardTaskResponse> tasks,
            @JsonFormat(pattern = ISO) LocalDateTime createdAt,
            @JsonFormat(pattern = ISO) LocalDateTime updatedAt) {}

    public record BoardColumnResponse(
            Long id,
            String name,
            int sortOrder,
            List<CardResponse> cards) {}

    public record BoardResponse(
            Long teamId,
            String teamName,
            List<BoardColumnResponse> columns) {}
}
