package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.TeamResponses.TeamResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.TeamMemberRepository;
import br.edu.ifsp.tcc.application.repository.TeamRepository;
import br.edu.ifsp.tcc.application.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private UserRepository userRepository;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private TeamService teamService;

    private User user(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        team.setName("Equipe " + id);
        return team;
    }

    private TeamMember member(Long id, Team team, User user, TeamRole role) {
        TeamMember member = new TeamMember();
        member.setId(id);
        member.setTeam(team);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private BoardColumn column(Long id, Team team, int order) {
        BoardColumn column = new BoardColumn();
        column.setId(id);
        column.setTeam(team);
        column.setName("Coluna " + id);
        column.setSortOrder(order);
        return column;
    }

    @Test
    void listTeams_shouldCreatePersonalTeamWithDefaultColumnsWhenUserHasNone() {
        User ana = user(1L, "Ana Souza", "ana@test.com");
        Team created = team(10L);
        when(teamRepository.save(any(Team.class))).thenReturn(created);
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of())
                .thenReturn(List.of(member(5L, created, ana, TeamRole.ADMIN)));
        when(kanbanItemService.findWithoutTeamByUserId(1L)).thenReturn(List.of());
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(1);

        List<TeamResponse> teams = teamService.listTeams(ana);

        ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(teamCaptor.capture());
        assertEquals("Estúdio de Ana", teamCaptor.getValue().getName());

        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        assertEquals(TeamRole.ADMIN, memberCaptor.getValue().getRole());

        ArgumentCaptor<BoardColumn> columnCaptor = ArgumentCaptor.forClass(BoardColumn.class);
        verify(boardColumnRepository, times(4)).save(columnCaptor.capture());
        assertEquals(TeamService.DEFAULT_COLUMNS,
                columnCaptor.getAllValues().stream().map(BoardColumn::getName).toList());

        assertEquals(1, teams.size());
        assertEquals(TeamRole.ADMIN, teams.get(0).role());
    }

    @Test
    void listTeams_shouldMoveItemsWithoutTeamIntoFirstTeam() {
        User ana = user(1L, "Ana", "ana@test.com");
        Team team = team(10L);
        BoardColumn first = column(100L, team, 0);
        BoardColumn last = column(103L, team, 3);
        KanbanItem idea = new KanbanItem();
        idea.setState(KanbanItemState.IDEATION);
        KanbanItem inProduction = new KanbanItem();
        inProduction.setState(KanbanItemState.IN_PRODUCTION);
        KanbanItem done = new KanbanItem();
        done.setState(KanbanItemState.DONE);

        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(member(5L, team, ana, TeamRole.ADMIN)));
        when(kanbanItemService.findWithoutTeamByUserId(1L)).thenReturn(List.of(idea, inProduction, done));
        when(boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(first, last));

        teamService.listTeams(ana);

        assertSame(team, idea.getTeam());
        assertNull(idea.getColumn(), "Ideias continuam na Caixa de Entrada");
        assertSame(first, inProduction.getColumn());
        assertSame(last, done.getColumn());
        verify(kanbanItemService, times(3)).save(any(KanbanItem.class));
        verify(teamRepository, never()).save(any());
    }

    @Test
    void requireMember_shouldThrowForbiddenWhenUserIsNotMember() {
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.requireMember(10L, 99L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireRole_shouldThrowForbiddenWhenRoleIsNotAllowed() {
        Team team = team(10L);
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.of(member(6L, team, user(2L, "Rui", "rui@test.com"), TeamRole.REVIEWER)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.requireRole(10L, 2L, TeamRole.ADMIN, TeamRole.CREATOR));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void addMember_shouldReturnNotFoundWhenEmailIsNotRegistered() {
        Team team = team(10L);
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(member(5L, team, user(1L, "Ana", "ana@test.com"), TeamRole.ADMIN)));
        when(userRepository.findByEmail("ninguem@test.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.addMember(10L, 1L, "ninguem@test.com", TeamRole.CREATOR));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void addMember_shouldRejectUserAlreadyInTeam() {
        Team team = team(10L);
        User rui = user(2L, "Rui", "rui@test.com");
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(member(5L, team, user(1L, "Ana", "ana@test.com"), TeamRole.ADMIN)));
        when(userRepository.findByEmail("rui@test.com")).thenReturn(Optional.of(rui));
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 2L))
                .thenReturn(Optional.of(member(6L, team, rui, TeamRole.CREATOR)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.addMember(10L, 1L, "rui@test.com", TeamRole.REVIEWER));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void addMember_shouldSaveMemberWithRequestedRole() {
        Team team = team(10L);
        User rui = user(2L, "Rui", "rui@test.com");
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 1L))
                .thenReturn(Optional.of(member(5L, team, user(1L, "Ana", "ana@test.com"), TeamRole.ADMIN)));
        when(userRepository.findByEmail("rui@test.com")).thenReturn(Optional.of(rui));
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 2L)).thenReturn(Optional.empty());
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = teamService.addMember(10L, 1L, "rui@test.com", TeamRole.REVIEWER);

        assertEquals(TeamRole.REVIEWER, response.role());
        assertEquals("rui@test.com", response.email());
    }

    @Test
    void updateMemberRole_shouldKeepAtLeastOneAdmin() {
        Team team = team(10L);
        TeamMember admin = member(5L, team, user(1L, "Ana", "ana@test.com"), TeamRole.ADMIN);
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(admin));
        when(teamMemberRepository.findByIdAndTeam_Id(5L, 10L)).thenReturn(Optional.of(admin));
        when(teamMemberRepository.countByTeam_IdAndRole(10L, TeamRole.ADMIN)).thenReturn(1);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.updateMemberRole(10L, 1L, 5L, TeamRole.CREATOR));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(TeamRole.ADMIN, admin.getRole());
    }

    @Test
    void removeMember_shouldForbidCreatorRemovingSomeoneElse() {
        Team team = team(10L);
        TeamMember creator = member(6L, team, user(2L, "Rui", "rui@test.com"), TeamRole.CREATOR);
        TeamMember other = member(7L, team, user(3L, "Bia", "bia@test.com"), TeamRole.CREATOR);
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(creator));
        when(teamMemberRepository.findByIdAndTeam_Id(7L, 10L)).thenReturn(Optional.of(other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.removeMember(10L, 2L, 7L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(teamMemberRepository, never()).delete(any());
    }

    @Test
    void removeMember_shouldLetMemberLeaveTheTeam() {
        Team team = team(10L);
        TeamMember creator = member(6L, team, user(2L, "Rui", "rui@test.com"), TeamRole.CREATOR);
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(creator));
        when(teamMemberRepository.findByIdAndTeam_Id(6L, 10L)).thenReturn(Optional.of(creator));

        teamService.removeMember(10L, 2L, 6L);

        verify(teamMemberRepository).delete(creator);
    }
}
