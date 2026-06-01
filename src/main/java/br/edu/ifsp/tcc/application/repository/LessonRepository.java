package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    Optional<Lesson> findByIdAndModule_Id(Long id, Long moduleId);
    int countByModule_Id(Long moduleId);
}
