package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {

    @Query("SELECT DISTINCT m FROM Module m LEFT JOIN FETCH m.lessons WHERE m.kanbanItem.id = :kanbanItemId ORDER BY m.sortOrder ASC")
    List<Module> findByKanbanItemIdOrderBySortOrderAsc(@Param("kanbanItemId") Long kanbanItemId);

    @Query("SELECT m FROM Module m LEFT JOIN FETCH m.lessons WHERE m.id = :id AND m.kanbanItem.id = :kanbanItemId")
    Optional<Module> findByIdAndKanbanItemId(@Param("id") Long id, @Param("kanbanItemId") Long kanbanItemId);

    int countByKanbanItemId(Long kanbanItemId);
}
