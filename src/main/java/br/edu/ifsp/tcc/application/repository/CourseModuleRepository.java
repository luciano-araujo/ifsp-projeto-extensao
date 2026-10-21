package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.CourseModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {
    List<CourseModule> findByCourse_IdOrderBySortOrderAscIdAsc(Long courseId);
    Optional<CourseModule> findByIdAndCourse_Id(Long id, Long courseId);
    int countByCourse_Id(Long courseId);
}
