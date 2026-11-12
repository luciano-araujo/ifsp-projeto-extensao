package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.CardMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CardMovementRepository extends JpaRepository<CardMovement, Long> {

    /** Date the card entered its current column: the latest movement of each card. */
    interface ColumnSince {
        Long getCardId();
        LocalDateTime getEnteredAt();
    }

    @Query("SELECT m FROM CardMovement m LEFT JOIN FETCH m.movedBy "
            + "WHERE m.kanbanItem.id = :cardId ORDER BY m.enteredAt DESC, m.id DESC")
    List<CardMovement> findHistory(@Param("cardId") Long cardId);

    @Query("SELECT m.kanbanItem.id AS cardId, MAX(m.enteredAt) AS enteredAt FROM CardMovement m "
            + "WHERE m.kanbanItem.id IN :cardIds GROUP BY m.kanbanItem.id")
    List<ColumnSince> findColumnSince(@Param("cardIds") Collection<Long> cardIds);
}
