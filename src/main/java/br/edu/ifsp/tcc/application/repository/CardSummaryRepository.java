package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.CardSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardSummaryRepository extends JpaRepository<CardSummary, Long> {

    Optional<CardSummary> findByKanbanItem_Id(Long kanbanItemId);
}
