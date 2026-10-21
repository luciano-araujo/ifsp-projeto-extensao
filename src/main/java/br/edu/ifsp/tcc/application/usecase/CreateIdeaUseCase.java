package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.CreateCardDTO;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CreateIdeaUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;

    public CreateIdeaUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
    }

    /** Saves an idea in the team inbox; audience and objective are optional until conversion. */
    @Transactional
    public CardResponse execute(Long teamId, User user, CreateCardDTO dto) {
        Team team = teamService.requireRole(teamId, user.getId(), TeamRole.ADMIN, TeamRole.CREATOR).getTeam();

        KanbanItem item = new KanbanItem();
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setTargetAudience(blankToNull(dto.getTargetAudience()));
        item.setPedagogicalObjective(blankToNull(dto.getPedagogicalObjective()));
        item.setTag(blankToNull(dto.getTag()));
        item.setState(KanbanItemState.IDEATION);
        item.setTeam(team);
        item.setUser(user);
        return boardService.toCardResponse(kanbanItemService.save(item), null, List.of());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
