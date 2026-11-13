package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.CardReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CardReviewRepository extends JpaRepository<CardReview, Long> {

    /** Reviews of the card, newest first (ids grow with time, so the id decides ties inside the same second). */
    @Query("SELECT r FROM CardReview r LEFT JOIN FETCH r.reviewer "
            + "WHERE r.kanbanItem.id = :cardId ORDER BY r.id DESC")
    List<CardReview> findByCard(@Param("cardId") Long cardId);

    /** The latest review of each of the given cards (one query, no N+1). */
    @Query("SELECT r FROM CardReview r WHERE r.kanbanItem.id IN :cardIds "
            + "AND r.id = (SELECT MAX(r2.id) FROM CardReview r2 WHERE r2.kanbanItem.id = r.kanbanItem.id)")
    List<CardReview> findLatestByCards(@Param("cardIds") Collection<Long> cardIds);
}
