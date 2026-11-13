package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    /** Number of materials of a card. */
    interface CardCount {
        Long getCardId();
        Long getTotal();
    }

    @Query("SELECT m FROM Material m LEFT JOIN FETCH m.uploadedBy "
            + "WHERE m.id = :id AND m.kanbanItem.team.id = :teamId")
    Optional<Material> findInTeam(@Param("id") Long id, @Param("teamId") Long teamId);

    @Query("SELECT m FROM Material m LEFT JOIN FETCH m.uploadedBy "
            + "WHERE m.kanbanItem.id = :cardId ORDER BY m.createdAt DESC, m.id DESC")
    List<Material> findByCardNewestFirst(@Param("cardId") Long cardId);

    int countByKanbanItem_Id(Long cardId);

    @Query("SELECT m.kanbanItem.id AS cardId, COUNT(m) AS total FROM Material m "
            + "WHERE m.kanbanItem.id IN :cardIds GROUP BY m.kanbanItem.id")
    List<CardCount> countByCards(@Param("cardIds") Collection<Long> cardIds);

    /** Total size in bytes of the materials of all the cards of a team (0 when there are none). */
    @Query("SELECT COALESCE(SUM(m.sizeBytes), 0) FROM Material m WHERE m.kanbanItem.team.id = :teamId")
    long sumSizeBytesByTeam(@Param("teamId") Long teamId);

    /** Materials linked to lessons of courses published on or before the limit, not archived yet (RN04). */
    @Query("SELECT DISTINCT m FROM LessonMaterial lm JOIN lm.material m JOIN lm.lesson l JOIN l.module mo JOIN mo.course c "
            + "WHERE c.status = br.edu.ifsp.tcc.application.entity.CourseStatus.PUBLISHED "
            + "AND c.publishedAt <= :limit AND m.archivedAt IS NULL ORDER BY m.id")
    List<Material> findDueForArchive(@Param("limit") java.time.LocalDateTime limit);

    /** Marks one material as archived in its own short transaction (call it outside any other transaction). */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Material m SET m.archivedAt = :at WHERE m.id = :id")
    int markArchived(@Param("id") Long id, @Param("at") java.time.LocalDateTime at);

    @Query("SELECT m.storageKey FROM Material m WHERE m.kanbanItem.id = :cardId")
    List<String> findStorageKeysByCard(@Param("cardId") Long cardId);

    @Query("SELECT m.storageKey FROM Material m WHERE m.kanbanItem.team.id = :teamId")
    List<String> findStorageKeysByTeam(@Param("teamId") Long teamId);
}
