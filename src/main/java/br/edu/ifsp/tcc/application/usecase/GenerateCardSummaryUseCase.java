package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardSummaryResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardSummaryService;
import br.edu.ifsp.tcc.application.exception.TooManyRequestsException;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.RateLimitService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.SummaryGenerator;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
public class GenerateCardSummaryUseCase {

    static final int MIN_WORDS = 100;

    private final TeamService teamService;
    private final BoardService boardService;
    private final ScriptService scriptService;
    private final CardSummaryService cardSummaryService;
    private final SummaryGenerator summaryGenerator;
    private final PlanService planService;
    private final RateLimitService rateLimitService;

    @Value("${app.summary.max-per-team-hour:10}")
    private int maxPerTeamHour = 10;

    public GenerateCardSummaryUseCase(TeamService teamService, BoardService boardService,
                                      ScriptService scriptService, CardSummaryService cardSummaryService,
                                      SummaryGenerator summaryGenerator, PlanService planService,
                                      RateLimitService rateLimitService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.scriptService = scriptService;
        this.cardSummaryService = cardSummaryService;
        this.summaryGenerator = summaryGenerator;
        this.planService = planService;
        this.rateLimitService = rateLimitService;
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
        planService.requireCanSummarize(item.getTeam());
        String plainText = scriptService.findByKanbanItemId(cardId).map(Script::getContent)
                .map(CardSummaryService::plainText).orElse("");
        if (CardSummaryService.countWords(plainText) < MIN_WORDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dados insuficientes para gerar o resumo: escreva pelo menos 100 palavras no roteiro.");
        }
        // Counted only for requests that would call the generator, per team (the path id), so one team cannot burn the AI service
        long retryAfter = rateLimitService.registerAttempt("summary:" + teamId, maxPerTeamHour, Duration.ofHours(1));
        if (retryAfter > 0) {
            throw new TooManyRequestsException("Limite de resumos da equipe atingido. Tente novamente mais tarde.", retryAfter);
        }
        String summary = summaryGenerator.summarize(CardSummaryService.buildPayload(item, plainText));
        return cardSummaryService.toResponse(cardSummaryService.overwrite(item, summary));
    }
}
