package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardColumnDTO;
import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class CreateColumnUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public CreateColumnUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    /** Only administrators configure the columns of the team board. */
    @Transactional
    public BoardColumnResponse execute(Long teamId, Long userId, BoardColumnDTO dto) {
        Team team = teamService.requireRole(teamId, userId, TeamRole.ADMIN).getTeam();
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da coluna e obrigatorio.");
        }

        BoardColumn column = new BoardColumn();
        column.setTeam(team);
        column.setName(dto.getName().trim());
        column.setSortOrder(boardService.countColumns(teamId));
        return boardService.toColumnResponse(boardService.saveColumn(column), List.of());
    }
}
