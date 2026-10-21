package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.IdeaVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IdeaVoteRepository extends JpaRepository<IdeaVote, Long> {
    List<IdeaVote> findByKanbanItem_IdIn(Collection<Long> kanbanItemIds);
    Optional<IdeaVote> findByKanbanItem_IdAndUser_Id(Long kanbanItemId, Long userId);
    int countByKanbanItem_Id(Long kanbanItemId);
}
