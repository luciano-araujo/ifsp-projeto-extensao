package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteUserUseCaseTest {

    @Mock private UserService userService;
    @Mock private TeamService teamService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private DeleteUserUseCase deleteUserUseCase;

    private TeamMember membership(Long teamId, String teamName, TeamRole role) {
        Team team = new Team();
        team.setId(teamId);
        team.setName(teamName);
        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setRole(role);
        return member;
    }

    @Test
    void execute_shouldDelegateToService() {
        when(teamService.findMembershipsByUserId(1L)).thenReturn(List.of());
        when(kanbanItemService.findWithoutTeamByUserId(1L)).thenReturn(List.of());

        deleteUserUseCase.execute(1L, 1L);

        verify(userService).deleteById(1L);
    }

    @Test
    void execute_shouldThrowForbiddenWhenUserIdDoesNotMatch() {
        assertThrows(ResponseStatusException.class, () -> deleteUserUseCase.execute(1L, 2L));
        verifyNoInteractions(userService, teamService, kanbanItemService);
    }

    @Test
    void execute_shouldBlockTheOnlyAdminOfASharedTeam() {
        when(teamService.findMembershipsByUserId(1L))
                .thenReturn(List.of(membership(10L, "Equipe Office Resolve", TeamRole.ADMIN)));
        when(teamService.countMembers(10L)).thenReturn(3);
        when(teamService.countAdmins(10L)).thenReturn(1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> deleteUserUseCase.execute(1L, 1L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Equipe Office Resolve"));
        verify(teamService, never()).deleteTeam(any());
        verify(userService, never()).deleteById(anyLong());
    }

    @Test
    void execute_shouldDeletePersonalTeamAndKeepSharedTeams() {
        TeamMember personal = membership(10L, "Estúdio de Ana", TeamRole.ADMIN);
        TeamMember shared = membership(20L, "Equipe Office Resolve", TeamRole.ADMIN);
        when(teamService.findMembershipsByUserId(1L)).thenReturn(List.of(personal, shared));
        when(teamService.countMembers(10L)).thenReturn(1);
        when(teamService.countMembers(20L)).thenReturn(3);
        when(teamService.countAdmins(20L)).thenReturn(2);
        when(kanbanItemService.findWithoutTeamByUserId(1L)).thenReturn(List.of());

        deleteUserUseCase.execute(1L, 1L);

        verify(teamService).deleteTeam(personal.getTeam());
        verify(teamService, never()).deleteTeam(shared.getTeam());
        verify(userService).deleteById(1L);
    }

    @Test
    void execute_shouldLetANonAdminMemberLeaveWithoutTouchingTheTeam() {
        TeamMember creator = membership(20L, "Equipe Office Resolve", TeamRole.CREATOR);
        when(teamService.findMembershipsByUserId(2L)).thenReturn(List.of(creator));
        when(teamService.countMembers(20L)).thenReturn(3);
        when(kanbanItemService.findWithoutTeamByUserId(2L)).thenReturn(List.of());

        deleteUserUseCase.execute(2L, 2L);

        verify(teamService, never()).deleteTeam(any());
        verify(userService).deleteById(2L);
    }

    @Test
    void execute_shouldDeleteCardsThatNeverJoinedATeam() {
        KanbanItem orphan = new KanbanItem();
        orphan.setId(99L);
        when(teamService.findMembershipsByUserId(1L)).thenReturn(List.of());
        when(kanbanItemService.findWithoutTeamByUserId(1L)).thenReturn(List.of(orphan));

        deleteUserUseCase.execute(1L, 1L);

        verify(kanbanItemService).deleteById(99L);
        verify(userService).deleteById(1L);
    }
}
