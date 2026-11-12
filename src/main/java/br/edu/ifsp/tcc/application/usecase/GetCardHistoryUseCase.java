package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardMovementResponse;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class GetCardHistoryUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public GetCardHistoryUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    /** Columns the card has been in, newest first. Any member can read it. */
    @Transactional(readOnly = true)
    public List<CardMovementResponse> execute(Long teamId, Long userId, Long cardId) {
        teamService.requireMember(teamId, userId);
        boardService.findCard(teamId, cardId);
        return boardService.findHistory(cardId).stream().map(boardService::toMovementResponse).toList();
    }
}
