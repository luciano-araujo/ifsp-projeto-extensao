package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.TeamMemberRepository;
import br.edu.ifsp.tcc.application.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @InjectMocks private TeamService teamService;

    private TeamMember member(TeamRole role) {
        TeamMember member = new TeamMember();
        member.setRole(role);
        return member;
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
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(member(TeamRole.REVIEWER)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> teamService.requireRole(10L, 2L, TeamRole.ADMIN, TeamRole.CREATOR));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireRole_shouldReturnMemberWhenRoleIsAllowed() {
        TeamMember creator = member(TeamRole.CREATOR);
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(creator));

        assertSame(creator, teamService.requireRole(10L, 2L, TeamRole.ADMIN, TeamRole.CREATOR));
    }

    @Test
    void createTeamWithAdmin_shouldCreateAdminAndDefaultColumns() {
        User ana = new User();
        ana.setId(1L);
        Team saved = new Team();
        saved.setId(10L);
        when(teamRepository.save(any(Team.class))).thenReturn(saved);
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamMember admin = teamService.createTeamWithAdmin(ana, "Equipe");

        assertEquals(TeamRole.ADMIN, admin.getRole());
        assertSame(ana, admin.getUser());
        ArgumentCaptor<BoardColumn> columns = ArgumentCaptor.forClass(BoardColumn.class);
        verify(boardColumnRepository, times(4)).save(columns.capture());
        assertEquals(TeamService.DEFAULT_COLUMNS, columns.getAllValues().stream().map(BoardColumn::getName).toList());
        assertEquals(3, columns.getAllValues().get(3).getSortOrder());
    }

    @Test
    void personalTeamName_shouldUseFirstName() {
        User ana = new User();
        ana.setName("Ana Souza");
        assertEquals("Estúdio de Ana", teamService.personalTeamName(ana));
    }
}
