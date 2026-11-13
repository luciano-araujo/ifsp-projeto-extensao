package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.dto.BoardResponse;
import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.CardReviewService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GetBoardUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;
    private final MaterialService materialService;
    private final CardReviewService cardReviewService;

    public GetBoardUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService,
                           MaterialService materialService, CardReviewService cardReviewService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
        this.materialService = materialService;
        this.cardReviewService = cardReviewService;
    }

    /** Columns of the team with their project cards; ideas stay in the inbox and are not listed here. */
    @Transactional(readOnly = true)
    public BoardResponse execute(Long teamId, Long userId) {
        Team team = teamService.requireMember(teamId, userId).getTeam();
        List<BoardColumn> columns = boardService.findColumns(teamId);
        List<KanbanItem> projects = kanbanItemService.findByTeamId(teamId).stream()
                .filter(item -> item.getState() != KanbanItemState.IDEATION)
                .toList();
        Map<Long, List<CardTask>> tasksByCard = boardService.findTasksByCard(projects);
        Map<Long, LocalDateTime> columnSinceByCard = boardService.findColumnSince(projects);
        Map<Long, Integer> materialsByCard = materialService.countByCards(projects);
        Map<Long, ReviewStatus> reviewStatusByCard = cardReviewService.findStatuses(projects);

        Long firstColumnId = columns.isEmpty() ? null : columns.get(0).getId();
        Map<Long, List<CardResponse>> cardsByColumn = new HashMap<>();
        for (KanbanItem item : projects) {
            // A project without a column (column deleted, or converted through the legacy route) shows in the first one
            Long columnId = item.getColumnId() != null ? item.getColumnId() : firstColumnId;
            if (columnId == null) continue;
            cardsByColumn.computeIfAbsent(columnId, id -> new ArrayList<>())
                    .add(boardService.toCardResponse(item, columnId, tasksByCard.getOrDefault(item.getId(), List.of()),
                            columnSinceByCard.get(item.getId()), materialsByCard.getOrDefault(item.getId(), 0),
                            reviewStatusByCard.getOrDefault(item.getId(), ReviewStatus.NONE)));
        }

        List<BoardColumnResponse> columnResponses = columns.stream()
                .map(column -> boardService.toColumnResponse(column, cardsByColumn.getOrDefault(column.getId(), List.of())))
                .toList();
        return new BoardResponse(team.getId(), team.getName(), columnResponses);
    }
}
