package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.VideoStatSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoStatSnapshotRepository extends JpaRepository<VideoStatSnapshot, Long> {

    Optional<VideoStatSnapshot> findFirstByKanbanItem_IdAndVideoIdOrderByCapturedAtDescIdDesc(Long kanbanItemId, String videoId);

    /** The 60 most recent snapshots of the card for that video, newest first (snapshots of a previous video never show). */
    List<VideoStatSnapshot> findTop60ByKanbanItem_IdAndVideoIdOrderByCapturedAtDescIdDesc(Long kanbanItemId, String videoId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM VideoStatSnapshot s WHERE s.kanbanItem.id = :cardId")
    int deleteByCardId(@Param("cardId") Long cardId);
}
