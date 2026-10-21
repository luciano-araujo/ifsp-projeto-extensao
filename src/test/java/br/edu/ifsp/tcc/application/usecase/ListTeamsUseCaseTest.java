package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListTeamsUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private ListTeamsUseCase useCase;

    private User ana() {
        User user = new User();
        user.setId(1L);
        user.setName("Ana Souza");
        return user;
    }

    private TeamMember membership(Team team) {
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setRole(TeamRole.ADMIN);
        return member;
    }

    @Test
    void execute_shouldCreatePersonalTeamWhenUserHasNone() {
        User ana = ana();
        Team team = new Team();
        team.setId(10L);
        when(teamService.findMembershipsByUserId(1L))
                .thenReturn(List.of())
                .thenReturn(List.of(membership(team)));
        when(teamService.personalTeamName(ana)).thenReturn("Estúdio de Ana");
        when(kanbanItemService.findWithoutTeamByUserId(1L)).thenReturn(List.of());

        useCase.execute(ana);

        verify(teamService).createTeamWithAdmin(ana, "Estúdio de Ana");
        verify(teamService).toTeamResponse(any(TeamMember.class));
    }

    @Test
    void execute_shouldMoveItemsWithoutTeamIntoFirstTeam() {
        User ana = ana();
        Team team = new Team();
        team.setId(10L);
        BoardColumn first = new BoardColumn();
        BoardColumn last = new BoardColumn();
        KanbanItem idea = new KanbanItem();
        idea.setState(KanbanItemState.IDEATION);
        KanbanItem inProduction = new KanbanItem();
        inProduction.setState(KanbanItemState.IN_PRODUCTION);
        KanbanItem done = new KanbanItem();
        done.setState(KanbanItemState.DONE);

        when(teamService.findMembershipsByUserId(1L)).thenReturn(List.of(membership(team)));
        when(kanbanItemService.findWithoutTeamByUserId(1L)).thenReturn(List.of(idea, inProduction, done));
        when(boardService.findColumns(10L)).thenReturn(List.of(first, last));

        useCase.execute(ana);

        assertSame(team, idea.getTeam());
        assertNull(idea.getColumn(), "Ideias continuam na Caixa de Entrada");
        assertSame(first, inProduction.getColumn());
        assertSame(last, done.getColumn());
        verify(kanbanItemService, times(3)).save(any(KanbanItem.class));
        verify(teamService, never()).createTeamWithAdmin(any(), any());
    }
}
