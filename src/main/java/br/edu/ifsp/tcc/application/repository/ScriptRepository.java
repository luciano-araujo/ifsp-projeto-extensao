package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.Script;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScriptRepository extends JpaRepository<Script, Long> {
    Optional<Script> findByKanbanItem_Id(Long kanbanItemId);
}
