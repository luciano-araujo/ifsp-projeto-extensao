package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.BoardColumnDTO;
import br.edu.ifsp.tcc.application.dto.ConvertCardDTO;
import br.edu.ifsp.tcc.application.dto.CreateCardDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponses.*;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Team board: configurable columns, the ideas inbox (Caixa de Entrada), project cards and their tasks.
 * Cards are kanban items, so script and curriculum routes keep working with the same ids.
 * Roles: ADMIN manages columns; ADMIN and CREATOR manage cards and tasks; every member can view
 * the board and tick tasks.
 */
@Service
public class BoardService {

    private static final TeamRole[] EDITORS = {TeamRole.ADMIN, TeamRole.CREATOR};

    private final TeamService teamService;
    private final KanbanItemService kanbanItemService;
    private final BoardColumnRepository boardColumnRepository;
    private final CardTaskRepository cardTaskRepository;

    public BoardService(TeamService teamService,
                        KanbanItemService kanbanItemService,
                        BoardColumnRepository boardColumnRepository,
                        CardTaskRepository cardTaskRepository) {
        this.teamService = teamService;
        this.kanbanItemService = kanbanItemService;
        this.boardColumnRepository = boardColumnRepository;
        this.cardTaskRepository = cardTaskRepository;
    }

    // --- Board and inbox ---

    @Transactional(readOnly = true)
    public BoardResponse getBoard(Long teamId, Long userId) {
        Team team = teamService.requireMember(teamId, userId).getTeam();
        List<BoardColumn> columns = boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(teamId);
        List<KanbanItem> projects = kanbanItemService.findByTeamId(teamId).stream()
                .filter(item -> item.getState() != KanbanItemState.IDEATION)
                .toList();
        Map<Long, List<CardTask>> tasksByCard = tasksByCard(projects);

        Long firstColumnId = columns.isEmpty() ? null : columns.get(0).getId();
        Map<Long, List<CardResponse>> cardsByColumn = new HashMap<>();
        for (KanbanItem item : projects) {
            // A project without a column (column deleted, or converted through the legacy route) shows in the first one
            Long columnId = item.getColumnId() != null ? item.getColumnId() : firstColumnId;
            if (columnId == null) continue;
            cardsByColumn.computeIfAbsent(columnId, id -> new ArrayList<>())
                    .add(toCardResponse(item, columnId, tasksByCard.getOrDefault(item.getId(), List.of())));
        }

        List<BoardColumnResponse> columnResponses = columns.stream()
                .map(column -> new BoardColumnResponse(column.getId(), column.getName(), column.getSortOrder(),
                        cardsByColumn.getOrDefault(column.getId(), List.of())))
                .toList();
        return new BoardResponse(team.getId(), team.getName(), columnResponses);
    }

    @Transactional(readOnly = true)
    public List<CardResponse> listIdeas(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return kanbanItemService.findByTeamId(teamId).stream()
                .filter(item -> item.getState() == KanbanItemState.IDEATION)
                .sorted(Comparator.comparing(KanbanItem::getId).reversed())
                .map(item -> toCardResponse(item, null, List.of()))
                .toList();
    }

    @Transactional
    public CardResponse createIdea(Long teamId, User user, CreateCardDTO dto) {
        Team team = teamService.requireRole(teamId, user.getId(), EDITORS).getTeam();

        KanbanItem item = new KanbanItem();
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setTargetAudience(blankToNull(dto.getTargetAudience()));
        item.setPedagogicalObjective(blankToNull(dto.getPedagogicalObjective()));
        item.setState(KanbanItemState.IDEATION);
        item.setTeam(team);
        item.setUser(user);
        return toCardResponse(kanbanItemService.save(item), null, List.of());
    }

    /** Creates a project directly on the board; RN01 applies as in a conversion. */
    @Transactional
    public CardResponse createCard(Long teamId, User user, CreateCardDTO dto) {
        Team team = teamService.requireRole(teamId, user.getId(), EDITORS).getTeam();
        requireRn01(dto.getTargetAudience(), dto.getPedagogicalObjective());
        BoardColumn column = resolveColumn(teamId, dto.getColumnId());

        KanbanItem item = new KanbanItem();
        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription());
        item.setTargetAudience(dto.getTargetAudience().trim());
        item.setPedagogicalObjective(dto.getPedagogicalObjective().trim());
        item.setState(KanbanItemState.IN_PRODUCTION);
        item.setTeam(team);
        item.setUser(user);
        item.setColumn(column);
        item.setSortOrder(nextSortOrder(teamId, column.getId()));
        KanbanItem saved = kanbanItemService.save(item);
        return toCardResponse(saved, column.getId(), List.of());
    }

    /** RN01: an idea only becomes a project with a target audience and a pedagogical objective. */
    @Transactional
    public CardResponse convertIdea(Long teamId, Long userId, Long cardId, Long targetColumnId, ConvertCardDTO dto) {
        teamService.requireRole(teamId, userId, EDITORS);
        KanbanItem item = findCard(teamId, cardId);
        if (item.getState() != KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Somente ideias da Caixa de Entrada podem ser convertidas.");
        }

        if (dto != null) {
            if (!isBlank(dto.getTitle())) item.setTitle(dto.getTitle().trim());
            if (!isBlank(dto.getTargetAudience())) item.setTargetAudience(dto.getTargetAudience().trim());
            if (!isBlank(dto.getPedagogicalObjective())) item.setPedagogicalObjective(dto.getPedagogicalObjective().trim());
        }
        requireRn01(item.getTargetAudience(), item.getPedagogicalObjective());

        BoardColumn column = resolveColumn(teamId, targetColumnId);
        item.setColumn(column);
        item.setSortOrder(nextSortOrder(teamId, column.getId()));
        item.setState(KanbanItemState.IN_PRODUCTION);
        KanbanItem saved = kanbanItemService.save(item);
        return toCardResponse(saved, column.getId(), List.of());
    }

    @Transactional
    public CardResponse moveCard(Long teamId, Long userId, Long cardId, Long columnId, Integer sortOrder) {
        teamService.requireRole(teamId, userId, EDITORS);
        KanbanItem item = findCard(teamId, cardId);
        if (item.getState() == KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Converta a ideia em projeto antes de move-la no quadro.");
        }
        BoardColumn column = resolveColumn(teamId, columnId);
        item.setColumn(column);
        item.setSortOrder(sortOrder != null ? sortOrder : nextSortOrder(teamId, column.getId()));
        KanbanItem saved = kanbanItemService.save(item);
        return toCardResponse(saved, column.getId(), tasksByCard(List.of(saved)).getOrDefault(saved.getId(), List.of()));
    }

    @Transactional
    public void deleteCard(Long teamId, Long userId, Long cardId) {
        teamService.requireRole(teamId, userId, EDITORS);
        KanbanItem item = findCard(teamId, cardId);
        kanbanItemService.deleteById(item.getId());
    }

    // --- Columns ---

    @Transactional
    public BoardColumnResponse addColumn(Long teamId, Long userId, BoardColumnDTO dto) {
        Team team = teamService.requireRole(teamId, userId, TeamRole.ADMIN).getTeam();
        if (isBlank(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da coluna e obrigatorio.");
        }
        BoardColumn column = new BoardColumn();
        column.setTeam(team);
        column.setName(dto.getName().trim());
        column.setSortOrder(boardColumnRepository.countByTeam_Id(teamId));
        BoardColumn saved = boardColumnRepository.save(column);
        return new BoardColumnResponse(saved.getId(), saved.getName(), saved.getSortOrder(), List.of());
    }

    @Transactional
    public BoardColumnResponse updateColumn(Long teamId, Long userId, Long columnId, BoardColumnDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        BoardColumn column = findColumn(teamId, columnId);
        if (!isBlank(dto.getName())) column.setName(dto.getName().trim());
        if (dto.getSortOrder() != null) column.setSortOrder(dto.getSortOrder());
        BoardColumn saved = boardColumnRepository.save(column);
        return new BoardColumnResponse(saved.getId(), saved.getName(), saved.getSortOrder(), List.of());
    }

    /** Deletes a column; its cards go to targetColumnId, or to the first remaining column. */
    @Transactional
    public void deleteColumn(Long teamId, Long userId, Long columnId, Long targetColumnId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        BoardColumn column = findColumn(teamId, columnId);

        List<BoardColumn> others = boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(teamId).stream()
                .filter(c -> !c.getId().equals(columnId))
                .toList();
        if (others.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O quadro precisa de pelo menos uma coluna.");
        }
        if (targetColumnId != null && targetColumnId.equals(columnId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha outra coluna para receber os cards.");
        }
        BoardColumn target = targetColumnId != null ? findColumn(teamId, targetColumnId) : others.get(0);

        for (KanbanItem item : kanbanItemService.findByTeamId(teamId)) {
            if (columnId.equals(item.getColumnId())) {
                item.setColumn(target);
                kanbanItemService.save(item);
            }
        }
        boardColumnRepository.delete(column);
    }

    // --- Tasks (checklist inside a card; card progress is computed from them) ---

    @Transactional
    public CardTaskResponse createTask(Long teamId, Long userId, Long cardId, String title) {
        teamService.requireRole(teamId, userId, EDITORS);
        KanbanItem item = findCard(teamId, cardId);

        CardTask task = new CardTask();
        task.setKanbanItem(item);
        task.setTitle(title.trim());
        task.setSortOrder(cardTaskRepository.countByKanbanItem_Id(cardId));
        CardTask saved = cardTaskRepository.save(task);
        refreshProgress(item);
        return toTaskResponse(saved);
    }

    @Transactional
    public CardTaskResponse toggleTask(Long teamId, Long userId, Long cardId, Long taskId) {
        teamService.requireMember(teamId, userId);
        KanbanItem item = findCard(teamId, cardId);
        CardTask task = findTask(cardId, taskId);
        task.setCompleted(!Boolean.TRUE.equals(task.getCompleted()));
        CardTask saved = cardTaskRepository.save(task);
        refreshProgress(item);
        return toTaskResponse(saved);
    }

    @Transactional
    public void deleteTask(Long teamId, Long userId, Long cardId, Long taskId) {
        teamService.requireRole(teamId, userId, EDITORS);
        KanbanItem item = findCard(teamId, cardId);
        cardTaskRepository.delete(findTask(cardId, taskId));
        refreshProgress(item);
    }

    /** Stores the computed progress so the legacy /kanban routes show the same value. */
    private void refreshProgress(KanbanItem item) {
        List<CardTask> tasks = tasksByCard(List.of(item)).getOrDefault(item.getId(), List.of());
        item.setProgress(progressOf(tasks));
        kanbanItemService.save(item);
    }

    static int progressOf(List<CardTask> tasks) {
        if (tasks.isEmpty()) return 0;
        long done = tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        return (int) Math.round(done * 100.0 / tasks.size());
    }

    // --- Helpers ---

    private KanbanItem findCard(Long teamId, Long cardId) {
        return kanbanItemService.findByIdAndTeamId(cardId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card nao encontrado."));
    }

    private BoardColumn findColumn(Long teamId, Long columnId) {
        return boardColumnRepository.findByIdAndTeam_Id(columnId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coluna nao encontrada."));
    }

    private CardTask findTask(Long cardId, Long taskId) {
        return cardTaskRepository.findByIdAndKanbanItem_Id(taskId, cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarefa nao encontrada."));
    }

    private BoardColumn resolveColumn(Long teamId, Long columnId) {
        if (columnId != null) return findColumn(teamId, columnId);
        return boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(teamId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "O quadro da equipe nao tem colunas."));
    }

    private int nextSortOrder(Long teamId, Long columnId) {
        return (int) kanbanItemService.findByTeamId(teamId).stream()
                .filter(item -> columnId.equals(item.getColumnId()))
                .count();
    }

    private Map<Long, List<CardTask>> tasksByCard(List<KanbanItem> items) {
        if (items.isEmpty()) return Map.of();
        List<Long> ids = items.stream().map(KanbanItem::getId).toList();
        return cardTaskRepository.findByKanbanItem_IdInOrderBySortOrderAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(CardTask::getKanbanItemId));
    }

    private void requireRn01(String targetAudience, String pedagogicalObjective) {
        if (isBlank(targetAudience) || isBlank(pedagogicalObjective)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para virar projeto, informe o publico-alvo e o objetivo pedagogico.");
        }
    }

    private CardResponse toCardResponse(KanbanItem item, Long columnId, List<CardTask> tasks) {
        int done = (int) tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        boolean idea = item.getState() == KanbanItemState.IDEATION;
        return new CardResponse(item.getId(), item.getTeamId(), item.getTitle(), item.getDescription(),
                idea ? "IDEA" : "PROJECT", columnId, item.getSortOrder() != null ? item.getSortOrder() : 0,
                progressOf(tasks), tasks.size(), done, item.getTargetAudience(), item.getPedagogicalObjective(),
                tasks.stream().map(this::toTaskResponse).toList(), item.getCreatedAt(), item.getUpdatedAt());
    }

    private CardTaskResponse toTaskResponse(CardTask task) {
        return new CardTaskResponse(task.getId(), task.getKanbanItemId(), task.getTitle(),
                Boolean.TRUE.equals(task.getCompleted()), task.getSortOrder() != null ? task.getSortOrder() : 0,
                task.getCreatedAt());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
