package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CardReviewResponse;
import br.edu.ifsp.tcc.application.dto.MovedByResponse;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.entity.CardReview;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.ReviewDecision;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.CardReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Data access and mapping of card reviews. Business rules live in the review use cases. */
@Service
public class CardReviewService {

    private final CardReviewRepository cardReviewRepository;

    public CardReviewService(CardReviewRepository cardReviewRepository) {
        this.cardReviewRepository = cardReviewRepository;
    }

    public CardReview record(KanbanItem item, User reviewer, ReviewDecision decision, String comment) {
        CardReview review = new CardReview();
        review.setKanbanItem(item);
        review.setReviewer(reviewer);
        review.setDecision(decision);
        review.setComment(comment);
        // Truncated to seconds, as the column: the response equals what is stored
        review.setCreatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return cardReviewRepository.save(review);
    }

    /** Reviews of the card, newest first. */
    public List<CardReview> findByCard(Long cardId) {
        return cardReviewRepository.findByCard(cardId);
    }

    /** Review status of each card (latest review); cards never reviewed are absent from the map (NONE). One query. */
    public Map<Long, ReviewStatus> findStatuses(List<KanbanItem> items) {
        Map<Long, ReviewStatus> statuses = new HashMap<>();
        if (items.isEmpty()) return statuses;
        for (CardReview review : cardReviewRepository.findLatestByCards(items.stream().map(KanbanItem::getId).toList())) {
            statuses.put(review.getKanbanItemId(), statusOf(review.getDecision()));
        }
        return statuses;
    }

    public static ReviewStatus statusOf(ReviewDecision decision) {
        return decision == ReviewDecision.APPROVED ? ReviewStatus.APPROVED : ReviewStatus.CHANGES_REQUESTED;
    }

    public CardReviewResponse toResponse(CardReview review) {
        User reviewer = review.getReviewer();
        return new CardReviewResponse(review.getId(), review.getKanbanItemId(), review.getDecision(),
                review.getComment(), reviewer == null ? null : new MovedByResponse(reviewer.getId(), reviewer.getName()),
                review.getCreatedAt());
    }
}
