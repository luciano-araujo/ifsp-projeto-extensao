package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KanbanItemService {

    private final KanbanItemRepository kanbanItemRepository;

    public KanbanItemService(KanbanItemRepository kanbanItemRepository) {
        this.kanbanItemRepository = kanbanItemRepository;
    }

    public KanbanItem save(KanbanItem item) {
        return kanbanItemRepository.save(item);
    }

    public List<KanbanItem> findByUserId(Long userId) {
        return kanbanItemRepository.findByUser_Id(userId);
    }

    public List<KanbanItem> findByUserIdAndState(Long userId, KanbanItemState state) {
        return kanbanItemRepository.findByUser_IdAndState(userId, state);
    }

    /**
     * Returns the item when the user owns it or is a member of the item's team.
     * Every kanban, script and curriculum route goes through this check.
     */
    public Optional<KanbanItem> findByIdAndUserId(Long id, Long userId) {
        return kanbanItemRepository.findAccessible(id, userId);
    }

    public List<KanbanItem> findByTeamId(Long teamId) {
        return kanbanItemRepository.findByTeam_IdOrderBySortOrderAscIdAsc(teamId);
    }

    public Optional<KanbanItem> findByIdAndTeamId(Long id, Long teamId) {
        return kanbanItemRepository.findByIdAndTeam_Id(id, teamId);
    }

    public List<KanbanItem> findWithoutTeamByUserId(Long userId) {
        return kanbanItemRepository.findByUser_IdAndTeamIsNull(userId);
    }

    public void deleteById(Long id) {
        kanbanItemRepository.deleteById(id);
    }
}
