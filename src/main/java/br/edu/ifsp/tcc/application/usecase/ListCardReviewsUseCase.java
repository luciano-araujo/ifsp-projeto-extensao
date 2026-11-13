package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardReviewResponse;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardReviewService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListCardReviewsUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final CardReviewService cardReviewService;

    public ListCardReviewsUseCase(TeamService teamService, BoardService boardService,
                                  CardReviewService cardReviewService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.cardReviewService = cardReviewService;
    }

    /** Reviews of the card, newest first. Any member can read them. */
    @Transactional(readOnly = true)
    public List<CardReviewResponse> execute(Long teamId, Long userId, Long cardId) {
        teamService.requireMember(teamId, userId);
        boardService.findCard(teamId, cardId);
        return cardReviewService.findByCard(cardId).stream().map(cardReviewService::toResponse).toList();
    }
}
