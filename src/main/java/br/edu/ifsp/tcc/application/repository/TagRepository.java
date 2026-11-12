package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByTeam_IdOrderByIdAsc(Long teamId);
    Optional<Tag> findByIdAndTeam_Id(Long id, Long teamId);
    Optional<Tag> findByTeam_IdAndNameIgnoreCase(Long teamId, String name);
}
