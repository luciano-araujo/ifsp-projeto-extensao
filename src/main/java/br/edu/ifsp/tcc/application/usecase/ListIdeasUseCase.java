package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
public class ListIdeasUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;

    public ListIdeasUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
    }

    /** Ideas of the team inbox (Caixa de Entrada), newest first. */
    @Transactional(readOnly = true)
    public List<CardResponse> execute(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return kanbanItemService.findByTeamId(teamId).stream()
                .filter(item -> item.getState() == KanbanItemState.IDEATION)
                .sorted(Comparator.comparing(KanbanItem::getId).reversed())
                .map(item -> boardService.toCardResponse(item, null, List.of()))
                .toList();
    }
}
