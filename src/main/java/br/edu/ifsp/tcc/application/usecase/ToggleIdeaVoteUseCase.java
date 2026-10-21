package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.IdeaVoteResponse;
import br.edu.ifsp.tcc.application.entity.IdeaVote;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Component
public class ToggleIdeaVoteUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public ToggleIdeaVoteUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    /** Any member votes once per inbox idea; voting again removes the vote. */
    @Transactional
    public IdeaVoteResponse execute(Long teamId, User user, Long cardId) {
        teamService.requireMember(teamId, user.getId());
        KanbanItem idea = boardService.findCard(teamId, cardId);
        if (idea.getState() != KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente ideias da Caixa de Entrada recebem votos.");
        }

        Optional<IdeaVote> existing = boardService.findVote(cardId, user.getId());
        if (existing.isPresent()) {
            boardService.deleteVote(existing.get());
        } else {
            IdeaVote vote = new IdeaVote();
            vote.setKanbanItem(idea);
            vote.setUser(user);
            boardService.saveVote(vote);
        }
        return new IdeaVoteResponse(cardId, boardService.countVotes(cardId), existing.isEmpty());
    }
}
