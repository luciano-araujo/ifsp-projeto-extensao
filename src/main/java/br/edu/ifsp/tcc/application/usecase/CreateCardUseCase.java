package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.CreateCardDTO;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class CreateCardUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;

    public CreateCardUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
    }

    /** Creates a project directly on the board; RN01 applies as in a conversion. */
    @Transactional
    public CardResponse execute(Long teamId, User user, CreateCardDTO dto) {
        Team team = teamService.requireRole(teamId, user.getId(), TeamRole.ADMIN, TeamRole.CREATOR).getTeam();
        if (isBlank(dto.getTargetAudience()) || isBlank(dto.getPedagogicalObjective())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para virar projeto, informe o publico-alvo e o objetivo pedagogico.");
        }
        BoardColumn column = boardService.resolveColumn(teamId, dto.getColumnId());

        KanbanItem item = new KanbanItem();
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setTargetAudience(dto.getTargetAudience().trim());
        item.setPedagogicalObjective(dto.getPedagogicalObjective().trim());
        item.setState(KanbanItemState.IN_PRODUCTION);
        item.setTeam(team);
        item.setUser(user);
        item.setColumn(column);
        item.setSortOrder(boardService.nextSortOrder(teamId, column.getId()));
        return boardService.toCardResponse(kanbanItemService.save(item), column.getId(), List.of());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
