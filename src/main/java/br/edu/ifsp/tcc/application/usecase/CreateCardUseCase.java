package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.CreateCardDTO;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TagService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CreateCardUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;
    private final TagService tagService;
    private final PlanService planService;

    public CreateCardUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService,
                             TagService tagService, PlanService planService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
        this.tagService = tagService;
        this.planService = planService;
    }

    /** Creates a project directly on the board; RN01 applies as in a conversion. */
    @Transactional
    public CardResponse execute(Long teamId, User user, CreateCardDTO dto) {
        Team team = teamService.requireRole(teamId, user.getId(), TeamRole.ADMIN, TeamRole.CREATOR).getTeam();
        KanbanItemService.requireProjectFields(dto.getTitle(), dto.getTargetAudience(), dto.getPedagogicalObjective());
        BoardColumn column = boardService.resolveColumn(teamId, dto.getColumnId());
        planService.requireCanCreateProject(team);

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
        item.setTag(tagService.resolveTag(teamId, dto.getTagId(), dto.getTag()));
        KanbanItem saved = kanbanItemService.save(item);
        CardMovement movement = boardService.recordMovement(saved, column, user);
        return boardService.toCardResponse(saved, column.getId(), List.of(), movement.getEnteredAt());
    }
}
