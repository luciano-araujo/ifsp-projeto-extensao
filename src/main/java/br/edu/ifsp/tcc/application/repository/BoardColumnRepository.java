package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {
    List<BoardColumn> findByTeam_IdOrderBySortOrderAscIdAsc(Long teamId);
    Optional<BoardColumn> findByIdAndTeam_Id(Long id, Long teamId);
    int countByTeam_Id(Long teamId);
}
