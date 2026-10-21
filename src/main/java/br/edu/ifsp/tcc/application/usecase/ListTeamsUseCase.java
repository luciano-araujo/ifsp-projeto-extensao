package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListTeamsUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final KanbanItemService kanbanItemService;

    public ListTeamsUseCase(TeamService teamService, BoardService boardService, KanbanItemService kanbanItemService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.kanbanItemService = kanbanItemService;
    }

    /**
     * Lists the teams of the user. A user without teams gets a personal team, and items created
     * before teams existed (or through the legacy /kanban route) are moved into the first team.
     */
    @Transactional
    public List<TeamResponse> execute(User user) {
        List<TeamMember> memberships = teamService.findMembershipsByUserId(user.getId());
        if (memberships.isEmpty()) {
            teamService.createTeamWithAdmin(user, teamService.personalTeamName(user));
            memberships = teamService.findMembershipsByUserId(user.getId());
        }
        adoptItemsWithoutTeam(user, memberships.get(0).getTeam());
        return memberships.stream().map(teamService::toTeamResponse).toList();
    }

    private void adoptItemsWithoutTeam(User user, Team team) {
        List<KanbanItem> orphans = kanbanItemService.findWithoutTeamByUserId(user.getId());
        if (orphans.isEmpty()) return;

        List<BoardColumn> columns = boardService.findColumns(team.getId());
        for (KanbanItem item : orphans) {
            item.setTeam(team);
            if (item.getState() != KanbanItemState.IDEATION && item.getColumn() == null && !columns.isEmpty()) {
                boolean late = item.getState() == KanbanItemState.REVIEW || item.getState() == KanbanItemState.DONE;
                item.setColumn(late ? columns.get(columns.size() - 1) : columns.get(0));
            }
            kanbanItemService.save(item);
        }
    }
}
