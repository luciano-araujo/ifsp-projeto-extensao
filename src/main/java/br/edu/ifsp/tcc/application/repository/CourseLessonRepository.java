package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.CourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseLessonRepository extends JpaRepository<CourseLesson, Long> {
    List<CourseLesson> findByModule_IdInOrderBySortOrderAscIdAsc(Collection<Long> moduleIds);
    Optional<CourseLesson> findByIdAndModule_Id(Long id, Long moduleId);
    int countByModule_Id(Long moduleId);
}
