package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardSummaryResponse;
import br.edu.ifsp.tcc.application.entity.CardSummary;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardSummaryService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.SummaryGenerator;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateCardSummaryUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private ScriptService scriptService;
    @Mock private CardSummaryService cardSummaryService;
    @Mock private SummaryGenerator summaryGenerator;
    @Mock private PlanService planService;
    @InjectMocks private GenerateCardSummaryUseCase useCase;

    private KanbanItem project;

    @BeforeEach
    void setUp() {
        project = new KanbanItem();
        project.setId(3L);
        project.setState(KanbanItemState.IN_PRODUCTION);
    }

    private String words(int n) {
        return String.join(" ", Collections.nCopies(n, "palavra"));
    }

    private Script script(String html) {
        Script script = new Script();
        script.setContent(html);
        return script;
    }

    @Test
    void execute_shouldSummarizePlainTextOfScriptAndSave() {
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        when(scriptService.findByKanbanItemId(3L)).thenReturn(Optional.of(script("<p>" + words(60) + "</p><p>" + words(60) + "</p>")));
        project.setTitle("Meu vídeo");
        project.setTargetAudience("Iniciantes");
        project.setPedagogicalObjective("   ");
        when(summaryGenerator.summarize("Título: Meu vídeo\nPúblico-alvo: Iniciantes\n\n" + words(60) + "\n" + words(60))).thenReturn("Resumo curto");
        CardSummary saved = new CardSummary();
        CardSummaryResponse response = mock(CardSummaryResponse.class);
        when(cardSummaryService.overwrite(project, "Resumo curto")).thenReturn(saved);
        when(cardSummaryService.toResponse(saved)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 3L));
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void execute_shouldAcceptExactlyOneHundredWords() {
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        when(scriptService.findByKanbanItemId(3L)).thenReturn(Optional.of(script("<p>" + words(100) + "</p>")));
        when(summaryGenerator.summarize(CardSummaryService.buildPayload(project, words(100)))).thenReturn("ok");

        useCase.execute(10L, 1L, 3L);

        verify(cardSummaryService).overwrite(project, "ok");
    }

    @Test
    void execute_shouldRejectNinetyNineWords() {
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        when(scriptService.findByKanbanItemId(3L)).thenReturn(Optional.of(script("<p>" + words(99) + "</p>")));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Dados insuficientes para gerar o resumo: escreva pelo menos 100 palavras no roteiro.", ex.getReason());
        verifyNoInteractions(summaryGenerator, cardSummaryService);
    }

    @Test
    void execute_shouldNotCountMarkupAsWords() {
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        String markup = "<p class=\"a b c d e f g h i j k l\" data-x=\"1 2 3 4 5 6 7 8 9 10\">" + words(50) + "</p>"
                + "<script>x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x x</script>";
        when(scriptService.findByKanbanItemId(3L)).thenReturn(Optional.of(script(markup)));
        when(boardService.findCard(10L, 3L)).thenReturn(project);

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
    }

    @Test
    void execute_shouldRejectProjectWithoutScript() {
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        when(scriptService.findByKanbanItemId(3L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(summaryGenerator);
    }

    @Test
    void execute_shouldRejectScriptWithNullContent() {
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        when(scriptService.findByKanbanItemId(3L)).thenReturn(Optional.of(script(null)));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        verifyNoInteractions(summaryGenerator);
    }

    @Test
    void execute_shouldRejectIdea() {
        project.setState(KanbanItemState.IDEATION);
        when(boardService.findCard(10L, 3L)).thenReturn(project);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Ideias não possuem roteiro para resumir.", ex.getReason());
        verifyNoInteractions(scriptService, summaryGenerator, cardSummaryService);
    }

    @Test
    void execute_shouldRejectReviewer() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        verifyNoInteractions(boardService, summaryGenerator);
    }

    @Test
    void execute_shouldKeepOldSummaryWhenGeneratorFails() {
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        when(scriptService.findByKanbanItemId(3L)).thenReturn(Optional.of(script("<p>" + words(120) + "</p>")));
        when(summaryGenerator.summarize(any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
        verifyNoInteractions(cardSummaryService);
    }


    @Test
    void execute_shouldStopWith402WhenThePlanHasNoAutomaticSummary() {
        br.edu.ifsp.tcc.application.entity.Team team = new br.edu.ifsp.tcc.application.entity.Team();
        project.setTeam(team);
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "limite")).when(planService).assertCanGenerateSummary(team);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
        verifyNoInteractions(summaryGenerator, cardSummaryService, scriptService);
    }
}
