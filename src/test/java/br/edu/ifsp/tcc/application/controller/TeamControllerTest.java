package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.PlanSource;
import br.edu.ifsp.tcc.application.entity.PlanType;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock private ListTeamsUseCase listTeamsUseCase;
    @Mock private CreateTeamUseCase createTeamUseCase;
    @Mock private ListTeamMembersUseCase listTeamMembersUseCase;
    @Mock private AddTeamMemberUseCase addTeamMemberUseCase;
    @Mock private UpdateTeamMemberRoleUseCase updateTeamMemberRoleUseCase;
    @Mock private RemoveTeamMemberUseCase removeTeamMemberUseCase;
    @Mock private RenameTeamUseCase renameTeamUseCase;
    @Mock private DeleteTeamUseCase deleteTeamUseCase;
    @InjectMocks private TeamController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    private TeamResponse team() {
        return new TeamResponse(10L, "Equipe", TeamRole.ADMIN, 1, LocalDateTime.now(), false, PlanType.GRATUITO, PlanSource.FREE, null);
    }

    private TeamMemberResponse member() {
        return new TeamMemberResponse(5L, 2L, "Bia", "bia@teste.com", TeamRole.CREATOR, LocalDateTime.now());
    }

    @Test
    void list_shouldReturnTeamsOfUser() {
        List<TeamResponse> teams = List.of(team());
        when(listTeamsUseCase.execute(user)).thenReturn(teams);

        ResponseEntity<List<TeamResponse>> response = controller.list(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(teams, response.getBody());
    }

    @Test
    void create_shouldReturnCreatedTeam() {
        CreateTeamDTO dto = new CreateTeamDTO();
        dto.setName("Equipe");
        TeamResponse created = team();
        when(createTeamUseCase.execute(user, dto)).thenReturn(created);

        ResponseEntity<TeamResponse> response = controller.create(user, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void listMembers_shouldPassTeamAndUser() {
        List<TeamMemberResponse> members = List.of(member());
        when(listTeamMembersUseCase.execute(10L, 1L)).thenReturn(members);

        ResponseEntity<List<TeamMemberResponse>> response = controller.listMembers(user, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(members, response.getBody());
    }

    @Test
    void addMember_shouldReturnTheInvite() {
        AddTeamMemberDTO dto = new AddTeamMemberDTO();
        dto.setEmail("bia@teste.com");
        InviteResponse added = new InviteResponse(3L, "bia@teste.com", TeamRole.CREATOR,
                br.edu.ifsp.tcc.application.entity.InviteStatus.PENDING, null, null, null);
        when(addTeamMemberUseCase.execute(10L, 1L, dto)).thenReturn(added);

        ResponseEntity<InviteResponse> response = controller.addMember(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(added, response.getBody());
    }

    @Test
    void updateMember_shouldReturnUpdatedMember() {
        UpdateTeamMemberDTO dto = new UpdateTeamMemberDTO();
        dto.setRole(TeamRole.REVIEWER);
        TeamMemberResponse updated = member();
        when(updateTeamMemberRoleUseCase.execute(10L, 1L, 5L, dto)).thenReturn(updated);

        ResponseEntity<TeamMemberResponse> response = controller.updateMember(user, 10L, 5L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
    }

    @Test
    void removeMember_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.removeMember(user, 10L, 5L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(removeTeamMemberUseCase).execute(10L, 1L, 5L);
    }

    @Test
    void rename_shouldReturnRenamedTeam() {
        RenameTeamDTO dto = new RenameTeamDTO();
        dto.setName("Novo nome");
        TeamResponse renamed = team();
        when(renameTeamUseCase.execute(10L, 1L, dto)).thenReturn(renamed);

        ResponseEntity<TeamResponse> response = controller.rename(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(renamed, response.getBody());
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.delete(user, 10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteTeamUseCase).execute(10L, 1L);
    }
}
