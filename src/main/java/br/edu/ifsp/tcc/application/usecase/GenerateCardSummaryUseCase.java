package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardSummaryResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardSummaryService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.SummaryGenerator;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GenerateCardSummaryUseCase {

    static final int MIN_WORDS = 100;

    private final TeamService teamService;
    private final BoardService boardService;
    private final ScriptService scriptService;
    private final CardSummaryService cardSummaryService;
    private final SummaryGenerator summaryGenerator;

    public GenerateCardSummaryUseCase(TeamService teamService, BoardService boardService,
                                      ScriptService scriptService, CardSummaryService cardSummaryService,
                                      SummaryGenerator summaryGenerator) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.scriptService = scriptService;
        this.cardSummaryService = cardSummaryService;
        this.summaryGenerator = summaryGenerator;
    }

    /**
     * Not transactional on purpose: the generator call can take 30 s and must not hold a database connection.
     * The script is read first, the generator is called, then the summary is saved in a short transaction.
     * When the generator fails, nothing is saved and the previous summary stays.
     */
    public CardSummaryResponse execute(Long teamId, Long userId, Long cardId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        KanbanItem item = boardService.findCard(teamId, cardId);
        if (item.getState() == KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ideias não possuem roteiro para resumir.");
        }
        String plainText = scriptService.findByKanbanItemId(cardId).map(Script::getContent)
                .map(CardSummaryService::plainText).orElse("");
        if (CardSummaryService.countWords(plainText) < MIN_WORDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dados insuficientes para gerar o resumo: escreva pelo menos 100 palavras no roteiro.");
        }
        String summary = summaryGenerator.summarize(CardSummaryService.buildPayload(item, plainText));
        return cardSummaryService.toResponse(cardSummaryService.overwrite(item, summary));
    }
}
