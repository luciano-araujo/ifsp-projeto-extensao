package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.LessonMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, Long> {

    // Material and uploader come in the same query, so mapping a whole course triggers no query per lesson
    @Query("SELECT lm FROM LessonMaterial lm JOIN FETCH lm.material m LEFT JOIN FETCH m.uploadedBy "
            + "WHERE lm.lesson.id IN :lessonIds ORDER BY lm.id ASC")
    List<LessonMaterial> findByLessons(@Param("lessonIds") Collection<Long> lessonIds);

    Optional<LessonMaterial> findByLesson_IdAndMaterial_Id(Long lessonId, Long materialId);

    boolean existsByLesson_IdAndMaterial_Id(Long lessonId, Long materialId);
}
