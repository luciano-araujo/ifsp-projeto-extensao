package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.CardTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardTaskRepository extends JpaRepository<CardTask, Long> {
    // Assignee and its user come in the same query, so mapping the board does not trigger one query per task
    @Query("SELECT t FROM CardTask t LEFT JOIN FETCH t.assignee a LEFT JOIN FETCH a.user "
            + "WHERE t.kanbanItem.id IN :kanbanItemIds ORDER BY t.sortOrder ASC, t.id ASC")
    List<CardTask> findByKanbanItem_IdInOrderBySortOrderAscIdAsc(@Param("kanbanItemIds") Collection<Long> kanbanItemIds);
    Optional<CardTask> findByIdAndKanbanItem_Id(Long id, Long kanbanItemId);
    int countByKanbanItem_Id(Long kanbanItemId);
}
