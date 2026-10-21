package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.ConvertCardDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class ConvertIdeaUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;

    public ConvertIdeaUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
    }

    /** RN01: an idea only becomes a project with a target audience and a pedagogical objective. */
    @Transactional
    public CardResponse execute(Long teamId, Long userId, Long cardId, Long targetColumnId, ConvertCardDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        KanbanItem item = boardService.findCard(teamId, cardId);
        if (item.getState() != KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente ideias da Caixa de Entrada podem ser convertidas.");
        }

        if (dto != null) {
            if (!isBlank(dto.getTitle())) item.setTitle(dto.getTitle().trim());
            if (!isBlank(dto.getTargetAudience())) item.setTargetAudience(dto.getTargetAudience().trim());
            if (!isBlank(dto.getPedagogicalObjective())) item.setPedagogicalObjective(dto.getPedagogicalObjective().trim());
        }
        if (isBlank(item.getTargetAudience()) || isBlank(item.getPedagogicalObjective())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para virar projeto, informe o publico-alvo e o objetivo pedagogico.");
        }

        BoardColumn column = boardService.resolveColumn(teamId, targetColumnId);
        item.setColumn(column);
        item.setSortOrder(boardService.nextSortOrder(teamId, column.getId()));
        item.setState(KanbanItemState.IN_PRODUCTION);
        return boardService.toCardResponse(kanbanItemService.save(item), column.getId(), List.of());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
