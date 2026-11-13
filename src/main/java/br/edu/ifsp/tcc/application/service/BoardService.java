package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.dto.CardMovementResponse;
import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.dto.MovedByResponse;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.dto.TaskAssigneeResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.CardMovementRepository;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import br.edu.ifsp.tcc.application.repository.IdeaVoteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data access and shared lookups of the team board: columns, cards (kanban items of a team)
 * and card tasks. Business rules live in the board use cases.
 */
@Service
public class BoardService {

    private final BoardColumnRepository boardColumnRepository;
    private final CardTaskRepository cardTaskRepository;
    private final IdeaVoteRepository ideaVoteRepository;
    private final KanbanItemService kanbanItemService;
    private final CardMovementRepository cardMovementRepository;

    public BoardService(BoardColumnRepository boardColumnRepository,
                        CardTaskRepository cardTaskRepository,
                        IdeaVoteRepository ideaVoteRepository,
                        KanbanItemService kanbanItemService,
                        CardMovementRepository cardMovementRepository) {
        this.boardColumnRepository = boardColumnRepository;
        this.cardTaskRepository = cardTaskRepository;
        this.ideaVoteRepository = ideaVoteRepository;
        this.kanbanItemService = kanbanItemService;
        this.cardMovementRepository = cardMovementRepository;
    }

    // --- Columns ---

    public List<BoardColumn> findColumns(Long teamId) {
        return boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(teamId);
    }

    public BoardColumn findColumn(Long teamId, Long columnId) {
        return boardColumnRepository.findByIdAndTeam_Id(columnId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coluna não encontrada."));
    }

    /** The given column, or the first column of the board when none is given. */
    public BoardColumn resolveColumn(Long teamId, Long columnId) {
        if (columnId != null) return findColumn(teamId, columnId);
        return findColumns(teamId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O quadro da equipe não tem colunas."));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card não encontrado."));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
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

    // --- Reordering ---

    /** A reorder request must list each existing id exactly once (no missing, extra or repeated ids). */
    public void requireExactIds(List<Long> requested, List<Long> existing, String message) {
        if (requested.size() != existing.size() || !Set.copyOf(requested).equals(Set.copyOf(existing))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    // --- Card history ---

    /** Registers that the card entered the column now; the column name is kept as a snapshot. */
    public CardMovement recordMovement(KanbanItem item, BoardColumn column, User movedBy) {
        CardMovement movement = new CardMovement();
        movement.setKanbanItem(item);
        movement.setColumn(column);
        movement.setColumnName(column.getName());
        movement.setMovedBy(movedBy);
        // Truncated to seconds: the column has second precision, so the response equals the stored history
        movement.setEnteredAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return cardMovementRepository.save(movement);
    }

    /** Movements of the card, newest first. */
    public List<CardMovement> findHistory(Long cardId) {
        return cardMovementRepository.findHistory(cardId);
    }

    /** When each card entered its current column (one query for all the cards, no N+1). */
    public Map<Long, LocalDateTime> findColumnSince(List<KanbanItem> items) {
        if (items.isEmpty()) return Map.of();
        return cardMovementRepository.findColumnSince(items.stream().map(KanbanItem::getId).toList()).stream()
                .collect(Collectors.toMap(CardMovementRepository.ColumnSince::getCardId,
                        CardMovementRepository.ColumnSince::getEnteredAt));
    }

    // --- Idea votes ---

    public List<IdeaVote> findVotes(List<KanbanItem> items) {
        if (items.isEmpty()) return List.of();
        return ideaVoteRepository.findByKanbanItem_IdIn(items.stream().map(KanbanItem::getId).toList());
    }

    public Optional<IdeaVote> findVote(Long cardId, Long userId) {
        return ideaVoteRepository.findByKanbanItem_IdAndUser_Id(cardId, userId);
    }

    public int countVotes(Long cardId) {
        return ideaVoteRepository.countByKanbanItem_Id(cardId);
    }

    public IdeaVote saveVote(IdeaVote vote) {
        return ideaVoteRepository.save(vote);
    }

    public void deleteVote(IdeaVote vote) {
        ideaVoteRepository.delete(vote);
    }

    public static int progressOf(List<CardTask> tasks) {
        if (tasks.isEmpty()) return 0;
        long done = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        return (int) Math.round(done * 100.0 / tasks.size());
    }

    // --- Response mapping ---

    public CardResponse toCardResponse(KanbanItem item, Long columnId, List<CardTask> tasks) {
        return toCardResponse(item, columnId, tasks, null);
    }

    public CardResponse toCardResponse(KanbanItem item, Long columnId, List<CardTask> tasks, LocalDateTime columnSince) {
        return toCardResponse(item, columnId, tasks, columnSince, ReviewStatus.NONE);
    }

    public CardResponse toCardResponse(KanbanItem item, Long columnId, List<CardTask> tasks, LocalDateTime columnSince,
                                       ReviewStatus reviewStatus) {
        int done = (int) tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        boolean idea = item.getState() == KanbanItemState.IDEATION;
        Tag tag = item.getTag();
        return new CardResponse(item.getId(), item.getUuid(), item.getTeamId(), item.getTitle(), item.getDescription(),
                idea ? "IDEA" : "PROJECT", columnId, item.getSortOrder() != null ? item.getSortOrder() : 0,
                progressOf(tasks), tasks.size(), done, item.getTargetAudience(), item.getPedagogicalObjective(),
                tasks.stream().map(this::toTaskResponse).toList(), tag != null ? tag.getName() : null,
                item.getDueDate(), 0, false, item.getCreatedAt(), item.getUpdatedAt(),
                tag != null ? tag.getId() : null, tag != null ? tag.getColor() : null, columnSince, reviewStatus);
    }

    public CardTaskResponse toTaskResponse(CardTask task) {
        TeamMember assignee = task.getAssignee();
        TaskAssigneeResponse assigneeResponse = assignee == null ? null
                : new TaskAssigneeResponse(assignee.getId(), assignee.getUser().getId(), assignee.getUser().getName());
        return new CardTaskResponse(task.getId(), task.getKanbanItemId(), task.getTitle(),
                Boolean.TRUE.equals(task.getCompleted()), task.getSortOrder() != null ? task.getSortOrder() : 0,
                task.getCreatedAt(), assigneeResponse, task.getDueDate());
    }

    public CardMovementResponse toMovementResponse(CardMovement movement) {
        User movedBy = movement.getMovedBy();
        return new CardMovementResponse(movement.getColumnId(), movement.getColumnName(), movement.getEnteredAt(),
                movedBy == null ? null : new MovedByResponse(movedBy.getId(), movedBy.getName()));
    }

    public BoardColumnResponse toColumnResponse(BoardColumn column, List<CardResponse> cards) {
        return new BoardColumnResponse(column.getId(), column.getName(), column.getSortOrder(), cards);
    }
}
