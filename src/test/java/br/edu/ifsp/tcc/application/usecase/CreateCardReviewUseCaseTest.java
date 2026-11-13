package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardReviewResponse;
import br.edu.ifsp.tcc.application.dto.CreateReviewDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.ReviewDecision;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.entity.CardReview;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardReviewService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCardReviewUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private CardReviewService cardReviewService;
    @InjectMocks private CreateCardReviewUseCase useCase;

    private User reviewer;

    @BeforeEach
    void setUp() {
        reviewer = new User();
        reviewer.setId(1L);
    }

    private void givenReviewerRole() {
        TeamMember member = new TeamMember();
        member.setUser(reviewer);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.REVIEWER)).thenReturn(member);
    }

    private KanbanItem card(KanbanItemState state) {
        KanbanItem item = new KanbanItem();
        item.setId(3L);
        item.setState(state);
        return item;
    }

    private CreateReviewDTO dto(ReviewDecision decision, String comment) {
        CreateReviewDTO dto = new CreateReviewDTO();
        dto.setDecision(decision);
        dto.setComment(comment);
        return dto;
    }

    @Test
    void execute_shouldRecordApprovalWithoutComment() {
        givenReviewerRole();
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        CardReview saved = new CardReview();
        CardReviewResponse response = mock(CardReviewResponse.class);
        when(cardReviewService.record(project, reviewer, ReviewDecision.APPROVED, null)).thenReturn(saved);
        when(cardReviewService.toResponse(saved)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 3L, dto(ReviewDecision.APPROVED, "   ")));
    }

    @Test
    void execute_shouldTrimCommentWhenRequestingChanges() {
        givenReviewerRole();
        KanbanItem project = card(KanbanItemState.IN_PRODUCTION);
        when(boardService.findCard(10L, 3L)).thenReturn(project);
        CardReview saved = new CardReview();
        when(cardReviewService.record(project, reviewer, ReviewDecision.CHANGES_REQUESTED, "Ajustar a introdução"))
                .thenReturn(saved);

        useCase.execute(10L, 1L, 3L, dto(ReviewDecision.CHANGES_REQUESTED, "  Ajustar a introdução "));

        verify(cardReviewService).record(project, reviewer, ReviewDecision.CHANGES_REQUESTED, "Ajustar a introdução");
    }

    @Test
    void execute_shouldRequireCommentWhenRequestingChanges() {
        givenReviewerRole();
        when(boardService.findCard(10L, 3L)).thenReturn(card(KanbanItemState.IN_PRODUCTION));

        for (String blank : new String[]{null, "", "  \n "}) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> useCase.execute(10L, 1L, 3L, dto(ReviewDecision.CHANGES_REQUESTED, blank)));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals("Informe um comentário ao solicitar alterações.", ex.getReason());
        }
        verify(cardReviewService, never()).record(any(), any(), any(), any());
    }

    @Test
    void execute_shouldRejectIdea() {
        givenReviewerRole();
        when(boardService.findCard(10L, 3L)).thenReturn(card(KanbanItemState.IDEATION));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, dto(ReviewDecision.APPROVED, null)));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Ideias não recebem revisão.", ex.getReason());
        verify(cardReviewService, never()).record(any(), any(), any(), any());
    }

    @Test
    void execute_shouldRejectCreator() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.REVIEWER))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));

        assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, dto(ReviewDecision.APPROVED, null)));
        verifyNoInteractions(boardService, cardReviewService);
    }
}
