package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KanbanItemRepository extends JpaRepository<KanbanItem, Long> {
    @EntityGraph(attributePaths = "tag")
    List<KanbanItem> findByTeam_IdOrderBySortOrderAscIdAsc(Long teamId);
    Optional<KanbanItem> findByIdAndTeam_Id(Long id, Long teamId);
    List<KanbanItem> findByTeam_IdAndState(Long teamId, KanbanItemState state);

    // One statement for all the cards (ideas included) of a team; see TeamService.deleteTeam
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM KanbanItem k WHERE k.team.id = :teamId")
    int deleteAllByTeamId(@Param("teamId") Long teamId);

    // Projects of the team with a due date up to the limit (overdue ones included), for the deadline alerts
    @Query("SELECT k FROM KanbanItem k WHERE k.team.id = :teamId "
            + "AND k.state = br.edu.ifsp.tcc.application.entity.KanbanItemState.IN_PRODUCTION "
            + "AND k.dueDate IS NOT NULL AND k.dueDate <= :limit")
    List<KanbanItem> findProjectsDueUntil(@Param("teamId") Long teamId, @Param("limit") java.time.LocalDate limit);

    // Same, for every team at once (daily digest); the team comes in the same query
    @Query("SELECT k FROM KanbanItem k JOIN FETCH k.team WHERE k.state = br.edu.ifsp.tcc.application.entity.KanbanItemState.IN_PRODUCTION "
            + "AND k.dueDate IS NOT NULL AND k.dueDate <= :limit")
    List<KanbanItem> findAllProjectsDueUntil(@Param("limit") java.time.LocalDate limit);

    // The item, only when the user is a member of the item's team
    @Query("SELECT k FROM KanbanItem k WHERE k.id = :id AND k.team.id IN "
            + "(SELECT m.team.id FROM TeamMember m WHERE m.user.id = :userId)")
    Optional<KanbanItem> findAccessible(@Param("id") Long id, @Param("userId") Long userId);
}
