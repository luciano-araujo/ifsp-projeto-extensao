package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.entity.IdeaVote;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ListIdeasUseCase {

    private static final String SORT_RECENT = "recent";
    private static final String SORT_VOTES = "votes";

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;

    public ListIdeasUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
    }

    /**
     * Ideas of the team inbox (Caixa de Entrada) with their vote count and the user's own vote.
     * Optional filters: q (title or description, case-insensitive) and tagId. Sort is "recent" (default,
     * newest first) or "votes" (most voted first, then newest).
     */
    @Transactional(readOnly = true)
    public List<CardResponse> execute(Long teamId, Long userId, String q, Long tagId, String sort) {
        teamService.requireMember(teamId, userId);
        boolean byVotes = isSortByVotes(sort);
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        List<KanbanItem> ideas = kanbanItemService.findByTeamId(teamId).stream()
                .filter(item -> item.getState() == KanbanItemState.IDEATION)
                .filter(item -> tagId == null || tagId.equals(item.getTagId()))
                .filter(item -> query.isEmpty() || contains(item.getTitle(), query) || contains(item.getDescription(), query))
                .toList();

        Map<Long, List<IdeaVote>> votesByIdea = boardService.findVotes(ideas).stream()
                .collect(Collectors.groupingBy(IdeaVote::getKanbanItemId));

        Comparator<KanbanItem> newestFirst = Comparator.comparing(KanbanItem::getId).reversed();
        Comparator<KanbanItem> order = byVotes
                ? Comparator.<KanbanItem>comparingInt(item -> votesByIdea.getOrDefault(item.getId(), List.of()).size())
                        .reversed().thenComparing(newestFirst)
                : newestFirst;

        return ideas.stream()
                .sorted(order)
                .map(item -> {
                    List<IdeaVote> votes = votesByIdea.getOrDefault(item.getId(), List.of());
                    CardResponse response = boardService.toCardResponse(item, null, List.of());
                    response.setVotes(votes.size());
                    response.setVotedByMe(votes.stream().anyMatch(vote -> userId.equals(vote.getUserId())));
                    return response;
                })
                .toList();
    }

    private static boolean isSortByVotes(String sort) {
        if (sort == null || sort.isBlank() || SORT_RECENT.equalsIgnoreCase(sort.trim())) return false;
        if (SORT_VOTES.equalsIgnoreCase(sort.trim())) return true;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordenação inválida. Use recent ou votes.");
    }

    private static boolean contains(String text, String lowerCaseQuery) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerCaseQuery);
    }
}
