package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CardReviewResponse;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.entity.CardReview;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.ReviewDecision;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.CardReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardReviewServiceTest {

    @Mock private CardReviewRepository repository;
    @InjectMocks private CardReviewService service;

    private KanbanItem card(Long id) {
        KanbanItem item = new KanbanItem();
        item.setId(id);
        return item;
    }

    private CardReview review(Long id, KanbanItem item, ReviewDecision decision) {
        CardReview review = new CardReview();
        review.setId(id);
        review.setKanbanItem(item);
        review.setDecision(decision);
        return review;
    }

    @Test
    void record_shouldSaveWithSecondPrecisionTimestamp() {
        User reviewer = new User();
        when(repository.save(any(CardReview.class))).thenAnswer(inv -> inv.getArgument(0));

        CardReview saved = service.record(card(3L), reviewer, ReviewDecision.CHANGES_REQUESTED, "Ajustar");

        assertSame(reviewer, saved.getReviewer());
        assertEquals(ReviewDecision.CHANGES_REQUESTED, saved.getDecision());
        assertEquals("Ajustar", saved.getComment());
        assertEquals(3L, saved.getKanbanItemId());
        assertEquals(0, saved.getCreatedAt().getNano());
    }

    @Test
    void findByCard_shouldDelegateToRepository() {
        CardReview review = new CardReview();
        when(repository.findByCard(3L)).thenReturn(List.of(review));

        assertEquals(List.of(review), service.findByCard(3L));
    }

    @Test
    void findStatuses_shouldMapLatestReviewOfEachCardInOneQuery() {
        KanbanItem a = card(1L);
        KanbanItem b = card(2L);
        KanbanItem c = card(3L);
        when(repository.findLatestByCards(List.of(1L, 2L, 3L))).thenReturn(List.of(
                review(10L, a, ReviewDecision.APPROVED), review(11L, b, ReviewDecision.CHANGES_REQUESTED)));

        Map<Long, ReviewStatus> statuses = service.findStatuses(List.of(a, b, c));

        assertEquals(ReviewStatus.APPROVED, statuses.get(1L));
        assertEquals(ReviewStatus.CHANGES_REQUESTED, statuses.get(2L));
        assertFalse(statuses.containsKey(3L));
        verify(repository, times(1)).findLatestByCards(anyList());
    }

    @Test
    void findStatuses_shouldSkipQueryForNoCards() {
        assertTrue(service.findStatuses(List.of()).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void toResponse_shouldMapReviewer() {
        User reviewer = new User();
        reviewer.setId(7L);
        reviewer.setName("Rui");
        CardReview review = review(5L, card(3L), ReviewDecision.APPROVED);
        review.setReviewer(reviewer);
        review.setCreatedAt(LocalDateTime.of(2026, 11, 12, 10, 0));

        CardReviewResponse response = service.toResponse(review);

        assertEquals(5L, response.getId());
        assertEquals(3L, response.getCardId());
        assertEquals(ReviewDecision.APPROVED, response.getDecision());
        assertEquals(7L, response.getReviewer().getUserId());
        assertEquals("Rui", response.getReviewer().getName());
    }

    @Test
    void toResponse_shouldReturnNullReviewerAfterAccountDeletion() {
        assertNull(service.toResponse(review(5L, card(3L), ReviewDecision.APPROVED)).getReviewer());
    }
}
