package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.entity.IdeaVote;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /** Ideas of the team inbox (Caixa de Entrada), newest first, with their vote count and the user's own vote. */
    @Transactional(readOnly = true)
    public List<CardResponse> execute(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        List<KanbanItem> ideas = kanbanItemService.findByTeamId(teamId).stream()
                .filter(item -> item.getState() == KanbanItemState.IDEATION)
                .sorted(Comparator.comparing(KanbanItem::getId).reversed())
                .toList();

        Map<Long, List<IdeaVote>> votesByIdea = boardService.findVotes(ideas).stream()
                .collect(Collectors.groupingBy(IdeaVote::getKanbanItemId));

        return ideas.stream()
                .map(item -> {
                    List<IdeaVote> votes = votesByIdea.getOrDefault(item.getId(), List.of());
                    CardResponse response = boardService.toCardResponse(item, null, List.of());
                    response.setVotes(votes.size());
                    response.setVotedByMe(votes.stream().anyMatch(vote -> userId.equals(vote.getUserId())));
                    return response;
                })
                .toList();
    }
}
