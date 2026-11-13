package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.TeamMemberResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.TeamMemberRepository;
import br.edu.ifsp.tcc.application.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceMoreTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private MaterialService materialService;
    @InjectMocks private TeamService service;

    private Team team(Long id) {
        Team team = new Team();
        team.setId(id);
        team.setName("Equipe");
        return team;
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail("user" + id + "@teste.com");
        return user;
    }

    private TeamMember member(Long id, Team team, User user, TeamRole role) {
        TeamMember member = new TeamMember();
        member.setId(id);
        member.setTeam(team);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    @Test
    void findMembershipsByUserId_shouldReturnMemberships() {
        List<TeamMember> memberships = List.of(member(1L, team(10L), user(1L, "Ana"), TeamRole.ADMIN));
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(1L)).thenReturn(memberships);

        assertSame(memberships, service.findMembershipsByUserId(1L));
    }

    @Test
    void findMembersByTeamId_shouldReturnMembers() {
        List<TeamMember> members = List.of(member(1L, team(10L), user(1L, "Ana"), TeamRole.ADMIN));
        when(teamMemberRepository.findByTeam_IdOrderByJoinedAtAsc(10L)).thenReturn(members);

        assertSame(members, service.findMembersByTeamId(10L));
    }

    @Test
    void findMemberByTeamIdAndUserId_shouldReturnOptionalMember() {
        TeamMember member = member(1L, team(10L), user(1L, "Ana"), TeamRole.ADMIN);
        when(teamMemberRepository.findByTeam_IdAndUser_Id(10L, 1L)).thenReturn(Optional.of(member));

        assertEquals(Optional.of(member), service.findMemberByTeamIdAndUserId(10L, 1L));
    }

    @Test
    void findMember_shouldReturnMemberOfTeam() {
        TeamMember member = member(3L, team(10L), user(1L, "Ana"), TeamRole.CREATOR);
        when(teamMemberRepository.findByIdAndTeam_Id(3L, 10L)).thenReturn(Optional.of(member));

        assertSame(member, service.findMember(10L, 3L));
    }

    @Test
    void findMember_shouldReturnNotFoundWhenMissing() {
        when(teamMemberRepository.findByIdAndTeam_Id(3L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findMember(10L, 3L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void countMembers_shouldReturnRepositoryCount() {
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(5);

        assertEquals(5, service.countMembers(10L));
    }

    @Test
    void countAdmins_shouldCountOnlyAdmins() {
        when(teamMemberRepository.countByTeam_IdAndRole(10L, TeamRole.ADMIN)).thenReturn(2);

        assertEquals(2, service.countAdmins(10L));
    }

    @Test
    void saveMember_shouldDelegateToRepository() {
        TeamMember member = member(null, team(10L), user(1L, "Ana"), TeamRole.REVIEWER);
        when(teamMemberRepository.save(member)).thenReturn(member);

        assertSame(member, service.saveMember(member));
    }

    @Test
    void deleteMember_shouldDelegateToRepository() {
        TeamMember member = member(3L, team(10L), user(1L, "Ana"), TeamRole.REVIEWER);

        service.deleteMember(member);

        verify(teamMemberRepository).delete(member);
    }

    @Test
    void deleteTeam_shouldDelegateToRepository() {
        Team team = team(10L);

        service.deleteTeam(team);

        verify(materialService).deleteAllOfTeam(10L);
        verify(teamRepository).delete(team);
    }

    @Test
    void personalTeamName_shouldUseFirstNameOfUser() {
        assertEquals("Estúdio de Ana", service.personalTeamName(user(1L, "  Ana Clara ")));
    }

    @Test
    void personalTeamName_shouldFallBackWhenUserHasNoName() {
        assertEquals("Meu Estúdio", service.personalTeamName(user(1L, null)));
        assertEquals("Meu Estúdio", service.personalTeamName(user(1L, "   ")));
    }

    @Test
    void toTeamResponse_shouldUseMembershipRoleAndMemberCount() {
        TeamMember membership = member(1L, team(10L), user(1L, "Ana"), TeamRole.ADMIN);
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(4);

        TeamResponse response = service.toTeamResponse(membership);

        assertEquals(10L, response.getId());
        assertEquals(TeamRole.ADMIN, response.getRole());
        assertEquals(4, response.getMembersCount());
    }

    @Test
    void toMemberResponse_shouldMapMemberAndUser() {
        TeamMember member = member(3L, team(10L), user(1L, "Ana"), TeamRole.CREATOR);

        TeamMemberResponse response = service.toMemberResponse(member);

        assertEquals(3L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("Ana", response.getName());
        assertEquals("user1@teste.com", response.getEmail());
        assertEquals(TeamRole.CREATOR, response.getRole());
    }
}
