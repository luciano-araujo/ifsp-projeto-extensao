package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardSummaryResponse;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardSummaryService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GetCardSummaryUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final CardSummaryService cardSummaryService;

    public GetCardSummaryUseCase(TeamService teamService, BoardService boardService,
                                 CardSummaryService cardSummaryService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.cardSummaryService = cardSummaryService;
    }

    /** The last summary generated for the card. Any member can read it. */
    @Transactional(readOnly = true)
    public CardSummaryResponse execute(Long teamId, Long userId, Long cardId) {
        teamService.requireMember(teamId, userId);
        boardService.findCard(teamId, cardId);
        return cardSummaryService.findByCard(cardId).map(cardSummaryService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhum resumo gerado para este projeto."));
    }
}
