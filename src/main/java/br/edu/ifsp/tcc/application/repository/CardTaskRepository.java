package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.CardTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardTaskRepository extends JpaRepository<CardTask, Long> {
    List<CardTask> findByKanbanItem_IdInOrderBySortOrderAscIdAsc(Collection<Long> kanbanItemIds);
    Optional<CardTask> findByIdAndKanbanItem_Id(Long id, Long kanbanItemId);
    int countByKanbanItem_Id(Long kanbanItemId);
}
