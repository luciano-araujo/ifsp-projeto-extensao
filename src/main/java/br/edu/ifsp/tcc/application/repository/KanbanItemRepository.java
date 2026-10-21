package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KanbanItemRepository extends JpaRepository<KanbanItem, Long> {
    List<KanbanItem> findByUser_Id(Long userId);
    List<KanbanItem> findByUser_IdAndState(Long userId, KanbanItemState state);
    Optional<KanbanItem> findByIdAndUser_Id(Long id, Long userId);

    List<KanbanItem> findByTeam_IdOrderBySortOrderAscIdAsc(Long teamId);
    Optional<KanbanItem> findByIdAndTeam_Id(Long id, Long teamId);
    List<KanbanItem> findByUser_IdAndTeamIsNull(Long userId);

    // Owner of the item, or member of the item's team
    @Query("SELECT k FROM KanbanItem k WHERE k.id = :id AND (k.user.id = :userId OR k.team.id IN "
            + "(SELECT m.team.id FROM TeamMember m WHERE m.user.id = :userId))")
    Optional<KanbanItem> findAccessible(@Param("id") Long id, @Param("userId") Long userId);
}
