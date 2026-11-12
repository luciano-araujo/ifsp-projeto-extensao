package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardColumnDTO;
import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class UpdateColumnUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public UpdateColumnUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    /** Renames or reorders a column. */
    @Transactional
    public BoardColumnResponse execute(Long teamId, Long userId, Long columnId, BoardColumnDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        BoardColumn column = boardService.findColumn(teamId, columnId);

        if (dto.getName() != null) {
            if (dto.getName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da coluna não pode ficar vazio.");
            }
            column.setName(dto.getName().trim());
        }
        if (dto.getSortOrder() != null) column.setSortOrder(dto.getSortOrder());
        return boardService.toColumnResponse(boardService.saveColumn(column), List.of());
    }
}
