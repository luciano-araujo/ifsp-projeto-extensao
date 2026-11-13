package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.ConvertCardDTO;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.CardMovement;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.PlanService;
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
    private final PlanService planService;

    public ConvertIdeaUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService,
                              PlanService planService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
        this.planService = planService;
    }

    /** RN01: an idea only becomes a project with a target audience and a pedagogical objective. */
    @Transactional
    public CardResponse execute(Long teamId, Long userId, Long cardId, Long targetColumnId, ConvertCardDTO dto) {
        User user = teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR).getUser();
        KanbanItem item = boardService.findCard(teamId, cardId);
        if (item.getState() != KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente ideias da Caixa de Entrada podem ser convertidas.");
        }

        if (dto != null) {
            if (!isBlank(dto.getTitle())) item.setTitle(dto.getTitle().trim());
            if (!isBlank(dto.getTargetAudience())) item.setTargetAudience(dto.getTargetAudience().trim());
            if (!isBlank(dto.getPedagogicalObjective())) item.setPedagogicalObjective(dto.getPedagogicalObjective().trim());
        }
        KanbanItemService.requireProjectFields(item.getTitle(), item.getTargetAudience(), item.getPedagogicalObjective());

        BoardColumn column = boardService.resolveColumn(teamId, targetColumnId);
        planService.requireCanCreateProject(item.getTeam());
        item.setColumn(column);
        item.setSortOrder(boardService.nextSortOrder(teamId, column.getId()));
        item.setState(KanbanItemState.IN_PRODUCTION);
        KanbanItem saved = kanbanItemService.save(item);
        CardMovement movement = boardService.recordMovement(saved, column, user);
        return boardService.toCardResponse(saved, column.getId(), List.of(), movement.getEnteredAt());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
