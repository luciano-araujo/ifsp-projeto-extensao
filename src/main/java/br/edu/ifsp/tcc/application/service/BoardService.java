package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Data access and shared lookups of the team board: columns, cards (kanban items of a team)
 * and card tasks. Business rules live in the board use cases.
 */
@Service
public class BoardService {

    private final BoardColumnRepository boardColumnRepository;
    private final CardTaskRepository cardTaskRepository;
    private final KanbanItemService kanbanItemService;

    public BoardService(BoardColumnRepository boardColumnRepository,
                        CardTaskRepository cardTaskRepository,
                        KanbanItemService kanbanItemService) {
        this.boardColumnRepository = boardColumnRepository;
        this.cardTaskRepository = cardTaskRepository;
        this.kanbanItemService = kanbanItemService;
    }

    // --- Columns ---

    public List<BoardColumn> findColumns(Long teamId) {
        return boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(teamId);
    }

    public BoardColumn findColumn(Long teamId, Long columnId) {
        return boardColumnRepository.findByIdAndTeam_Id(columnId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coluna nao encontrada."));
    }

    /** The given column, or the first column of the board when none is given. */
    public BoardColumn resolveColumn(Long teamId, Long columnId) {
        if (columnId != null) return findColumn(teamId, columnId);
        return findColumns(teamId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O quadro da equipe nao tem colunas."));
    }

    public int countColumns(Long teamId) {
        return boardColumnRepository.countByTeam_Id(teamId);
    }

    public BoardColumn saveColumn(BoardColumn column) {
        return boardColumnRepository.save(column);
    }

    public void deleteColumn(BoardColumn column) {
        boardColumnRepository.delete(column);
    }

    // --- Cards ---

    public KanbanItem findCard(Long teamId, Long cardId) {
        return kanbanItemService.findByIdAndTeamId(cardId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card nao encontrado."));
    }

    public int nextSortOrder(Long teamId, Long columnId) {
        return (int) kanbanItemService.findByTeamId(teamId).stream()
                .filter(item -> columnId.equals(item.getColumnId()))
                .count();
    }

    // --- Tasks ---

    public Map<Long, List<CardTask>> findTasksByCard(List<KanbanItem> items) {
        if (items.isEmpty()) return Map.of();
        List<Long> ids = items.stream().map(KanbanItem::getId).toList();
        return cardTaskRepository.findByKanbanItem_IdInOrderBySortOrderAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(CardTask::getKanbanItemId));
    }

    public List<CardTask> findTasks(KanbanItem item) {
        return findTasksByCard(List.of(item)).getOrDefault(item.getId(), List.of());
    }

    public CardTask findTask(Long cardId, Long taskId) {
        return cardTaskRepository.findByIdAndKanbanItem_Id(taskId, cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa nao encontrada."));
    }

    public int countTasks(Long cardId) {
        return cardTaskRepository.countByKanbanItem_Id(cardId);
    }

    public CardTask saveTask(CardTask task) {
        return cardTaskRepository.save(task);
    }

    public void deleteTask(CardTask task) {
        cardTaskRepository.delete(task);
    }

    /** Stores the progress computed from the tasks, so the legacy /kanban routes show the same value. */
    public void refreshProgress(KanbanItem item) {
        item.setProgress(progressOf(findTasks(item)));
        kanbanItemService.save(item);
    }

    public static int progressOf(List<CardTask> tasks) {
        if (tasks.isEmpty()) return 0;
        long done = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        return (int) Math.round(done * 100.0 / tasks.size());
    }

    // --- Response mapping ---

    public CardResponse toCardResponse(KanbanItem item, Long columnId, List<CardTask> tasks) {
        int done = (int) tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        boolean idea = item.getState() == KanbanItemState.IDEATION;
        return new CardResponse(item.getId(), item.getTeamId(), item.getTitle(), item.getDescription(),
                idea ? "IDEA" : "PROJECT", columnId, item.getSortOrder() != null ? item.getSortOrder() : 0,
                progressOf(tasks), tasks.size(), done, item.getTargetAudience(), item.getPedagogicalObjective(),
                tasks.stream().map(this::toTaskResponse).toList(), item.getCreatedAt(), item.getUpdatedAt());
    }

    public CardTaskResponse toTaskResponse(CardTask task) {
        return new CardTaskResponse(task.getId(), task.getKanbanItemId(), task.getTitle(),
                Boolean.TRUE.equals(task.getCompleted()), task.getSortOrder() != null ? task.getSortOrder() : 0,
                task.getCreatedAt());
    }

    public BoardColumnResponse toColumnResponse(BoardColumn column, List<CardResponse> cards) {
        return new BoardColumnResponse(column.getId(), column.getName(), column.getSortOrder(), cards);
    }
}
