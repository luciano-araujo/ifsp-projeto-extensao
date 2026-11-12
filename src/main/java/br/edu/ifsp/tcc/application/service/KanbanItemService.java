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

    /** RN01 minimums (product decision 7), shared by create, convert and edit. */
    public static final int MIN_AUDIENCE = 3;
    public static final int MIN_OBJECTIVE = 10;

    public static void requireAudience(String audience) {
        if (audience == null || audience.trim().length() < MIN_AUDIENCE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o público-alvo com pelo menos " + MIN_AUDIENCE + " caracteres.");
        }
    }

    public static void requireObjective(String objective) {
        if (objective == null || objective.trim().length() < MIN_OBJECTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe o objetivo pedagógico com pelo menos " + MIN_OBJECTIVE + " caracteres.");
        }
    }

    /** RN01 for a card that becomes (or is created as) a project: effective values, after trim. */
    public static void requireProjectFields(String title, String audience, String objective) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O título não pode ficar vazio.");
        }
        requireAudience(audience);
        requireObjective(objective);
    }

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
