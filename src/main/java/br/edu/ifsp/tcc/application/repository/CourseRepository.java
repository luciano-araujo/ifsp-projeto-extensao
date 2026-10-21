package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeam_IdOrderByCreatedAtDesc(Long teamId);
    Optional<Course> findByIdAndTeam_Id(Long id, Long teamId);
}
