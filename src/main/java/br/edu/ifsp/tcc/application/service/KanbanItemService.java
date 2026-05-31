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
        return kanbanItemRepository.findByUserId(userId);
    }

    public List<KanbanItem> findByUserIdAndState(Long userId, KanbanItemState state) {
        return kanbanItemRepository.findByUserIdAndState(userId, state);
    }

    public Optional<KanbanItem> findByIdAndUserId(Long id, Long userId) {
        return kanbanItemRepository.findByIdAndUserId(id, userId);
    }

    public void deleteById(Long id) {
        kanbanItemRepository.deleteById(id);
    }
}
