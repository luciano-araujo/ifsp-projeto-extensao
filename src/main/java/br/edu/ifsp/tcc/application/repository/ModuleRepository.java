package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Long> {
    List<Module> findByKanbanItemIdOrderBySortOrderAsc(Long kanbanItemId);
    Optional<Module> findByIdAndKanbanItemId(Long id, Long kanbanItemId);
    int countByKanbanItemId(Long kanbanItemId);
}
