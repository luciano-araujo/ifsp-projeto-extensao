package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.dto.ReorderColumnsDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReorderColumnsUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public ReorderColumnsUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    /** Only administrators reorder; the list must contain exactly the columns of the team. */
    @Transactional
    public List<BoardColumnResponse> execute(Long teamId, Long userId, ReorderColumnsDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        List<BoardColumn> columns = boardService.findColumns(teamId);
        boardService.requireExactIds(dto.getColumnIds(), columns.stream().map(BoardColumn::getId).toList(),
                "A lista deve conter exatamente as colunas da equipe.");

        Map<Long, BoardColumn> byId = columns.stream().collect(Collectors.toMap(BoardColumn::getId, Function.identity()));
        List<BoardColumnResponse> ordered = new ArrayList<>();
        for (int i = 0; i < dto.getColumnIds().size(); i++) {
            BoardColumn column = byId.get(dto.getColumnIds().get(i));
            column.setSortOrder(i);
            ordered.add(boardService.toColumnResponse(boardService.saveColumn(column), List.of()));
        }
        return ordered;
    }
}
