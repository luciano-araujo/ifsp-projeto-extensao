package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Returns the card only when the user is a member of the card's team; otherwise 404, so the
     * existence of other teams' cards is not revealed. Every /kanban/{id} route goes through this check.
     */
    public KanbanItem findCardOfMember(Long cardId, Long userId) {
        return kanbanItemRepository.findAccessible(cardId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card não encontrado."));
    }

    public List<KanbanItem> findByTeamId(Long teamId) {
        return kanbanItemRepository.findByTeam_IdOrderBySortOrderAscIdAsc(teamId);
    }

    public Optional<KanbanItem> findByIdAndTeamId(Long id, Long teamId) {
        return kanbanItemRepository.findByIdAndTeam_Id(id, teamId);
    }

    public void deleteById(Long id) {
        kanbanItemRepository.deleteById(id);
    }
}
