package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardReviewResponse;
import br.edu.ifsp.tcc.application.dto.CreateReviewDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.ReviewDecision;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardReviewService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CreateCardReviewUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final CardReviewService cardReviewService;

    public CreateCardReviewUseCase(TeamService teamService, BoardService boardService,
                                   CardReviewService cardReviewService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.cardReviewService = cardReviewService;
    }

    /** ADMIN or REVIEWER registers a decision on a project; asking for changes needs a comment. */
    @Transactional
    public CardReviewResponse execute(Long teamId, Long userId, Long cardId, CreateReviewDTO dto) {
        User reviewer = teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.REVIEWER).getUser();
        KanbanItem item = boardService.findCard(teamId, cardId);
        if (item.getState() == KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ideias não recebem revisão.");
        }
        String comment = dto.getComment() == null || dto.getComment().isBlank() ? null : dto.getComment().trim();
        if (dto.getDecision() == ReviewDecision.CHANGES_REQUESTED && comment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um comentário ao solicitar alterações.");
        }
        return cardReviewService.toResponse(cardReviewService.record(item, reviewer, dto.getDecision(), comment));
    }
}
