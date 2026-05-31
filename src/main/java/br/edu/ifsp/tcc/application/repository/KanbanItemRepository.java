package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KanbanItemRepository extends JpaRepository<KanbanItem, Long> {
    List<KanbanItem> findByUserId(Long userId);
    List<KanbanItem> findByUserIdAndState(Long userId, KanbanItemState state);
    Optional<KanbanItem> findByIdAndUserId(Long id, Long userId);
}
