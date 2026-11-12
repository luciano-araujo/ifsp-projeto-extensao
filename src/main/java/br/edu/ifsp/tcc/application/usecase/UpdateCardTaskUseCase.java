package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.dto.UpdateTaskDTO;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.LocalDate;

@Component
public class UpdateCardTaskUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public UpdateCardTaskUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    /**
     * Partial update of a task: title, assignee (a member of the team; 0 removes it) and due date
     * (an empty string removes it). Fields that are not sent stay as they are.
     */
    @Transactional
    public CardTaskResponse execute(Long teamId, Long userId, Long cardId, Long taskId, UpdateTaskDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        boardService.findCard(teamId, cardId);
        CardTask task = boardService.findTask(cardId, taskId);

        if (dto.getTitle() != null) {
            if (dto.getTitle().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título da tarefa não pode ficar vazio.");
            }
            task.setTitle(dto.getTitle().trim());
        }
        if (dto.getAssigneeMemberId() != null) {
            if (dto.getAssigneeMemberId() == 0) {
                task.setAssignee(null);
            } else {
                task.setAssignee(teamService.findMemberOfTeam(teamId, dto.getAssigneeMemberId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "O responsável precisa ser um membro da equipe.")));
            }
        }
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate().isEmpty() ? null : parseDate(dto.getDueDate()));
        }
        return boardService.toTaskResponse(boardService.saveTask(task));
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prazo inválido. Use o formato AAAA-MM-DD.");
        }
    }
}
