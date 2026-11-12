package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.dto.ReorderTasksDTO;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReorderCardTasksUseCase {

    private final TeamService teamService;
    private final BoardService boardService;

    public ReorderCardTasksUseCase(TeamService teamService, BoardService boardService) {
        this.teamService = teamService;
        this.boardService = boardService;
    }

    /** The list must contain exactly the tasks of the card; returns them in the new order. */
    @Transactional
    public List<CardTaskResponse> execute(Long teamId, Long userId, Long cardId, ReorderTasksDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        KanbanItem item = boardService.findCard(teamId, cardId);
        List<CardTask> tasks = boardService.findTasks(item);
        boardService.requireExactIds(dto.getTaskIds(), tasks.stream().map(CardTask::getId).toList(),
                "A lista deve conter exatamente as tarefas do card.");

        Map<Long, CardTask> byId = tasks.stream().collect(Collectors.toMap(CardTask::getId, Function.identity()));
        List<CardTaskResponse> ordered = new ArrayList<>();
        for (int i = 0; i < dto.getTaskIds().size(); i++) {
            CardTask task = byId.get(dto.getTaskIds().get(i));
            task.setSortOrder(i);
            ordered.add(boardService.toTaskResponse(boardService.saveTask(task)));
        }
        return ordered;
    }
}
