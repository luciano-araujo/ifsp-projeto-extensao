package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateInviteDTO;
import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateInviteUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private UserService userService;
    @Mock private InviteService inviteService;
    @Mock private EmailService emailService;
    @Mock private PlanService planService;
    @InjectMocks private CreateInviteUseCase useCase;

    private TeamMember admin;

    @BeforeEach
    void setUp() {
        Team team = new Team();
        team.setId(10L);
        team.setName("Equipe Office");
        User ana = new User();
        ana.setId(1L);
        ana.setName("Ana Admin");
        admin = new TeamMember();
        admin.setTeam(team);
        admin.setUser(ana);
        admin.setRole(TeamRole.ADMIN);
    }

    private CreateInviteDTO dto(String email, TeamRole role) {
        CreateInviteDTO dto = new CreateInviteDTO();
        dto.setEmail(email);
        dto.setRole(role);
        return dto;
    }

    private TeamInvite invite(LocalDateTime expiresAt) {
        TeamInvite invite = new TeamInvite();
        invite.setExpiresAt(expiresAt);
        return invite;
    }

    @Test
    void execute_shouldCreateInviteAndSendTheTokenByEmail() {
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(teamService);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(userService.findByEmail("carla@exemplo.test")).thenReturn(Optional.empty());
        when(inviteService.findPendingByTeamAndEmail(10L, "carla@exemplo.test")).thenReturn(List.of());
        when(inviteService.generateToken()).thenReturn("token-123");
        when(inviteService.create(any(TeamInvite.class), eq("token-123"))).thenAnswer(inv -> inv.getArgument(0));
        InviteResponse response = new InviteResponse(3L, "carla@exemplo.test", TeamRole.REVIEWER,
                InviteStatus.PENDING, null, null, null);
        when(inviteService.toInviteResponse(any(TeamInvite.class))).thenReturn(response);

        InviteResponse result = useCase.execute(10L, 1L, dto("  carla@exemplo.test ", TeamRole.REVIEWER));

        assertSame(response, result);
        ArgumentCaptor<TeamInvite> saved = ArgumentCaptor.forClass(TeamInvite.class);
        verify(inviteService).create(saved.capture(), eq("token-123"));
        // the team row is locked before anything else is read
        order.verify(teamService).lockTeam(10L);
        order.verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN);
        assertEquals("carla@exemplo.test", saved.getValue().getEmail());
        assertEquals(TeamRole.REVIEWER, saved.getValue().getRole());
        assertEquals(InviteStatus.PENDING, saved.getValue().getStatus());
        assertSame(admin.getUser(), saved.getValue().getInvitedBy());
        verify(emailService).sendTeamInvite("carla@exemplo.test", "Ana Admin", "Equipe Office", "Revisor", "token-123");
    }

    @Test
    void execute_shouldDefaultToCreatorWhenRoleIsMissing() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(userService.findByEmail("x@exemplo.test")).thenReturn(Optional.empty());
        when(inviteService.findPendingByTeamAndEmail(10L, "x@exemplo.test")).thenReturn(List.of());
        when(inviteService.generateToken()).thenReturn("t");
        when(inviteService.create(any(TeamInvite.class), eq("t"))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(10L, 1L, dto("x@exemplo.test", null));

        verify(emailService).sendTeamInvite("x@exemplo.test", "Ana Admin", "Equipe Office", "Criador", "t");
    }

    @Test
    void execute_shouldLabelTheAdministratorRole() {
        assertEquals("Administrador", CreateInviteUseCase.roleLabel(TeamRole.ADMIN));
    }

    @Test
    void execute_shouldRejectEmailOfAnExistingMemberWith409() {
        User bia = new User();
        bia.setId(2L);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(userService.findByEmail("bia@exemplo.test")).thenReturn(Optional.of(bia));
        when(teamService.findMemberByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(new TeamMember()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("bia@exemplo.test", TeamRole.CREATOR)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(emailService, never()).sendTeamInvite(any(), any(), any(), any(), any());
    }

    @Test
    void execute_shouldInviteARegisteredUserWhoIsNotYetAMember() {
        User bia = new User();
        bia.setId(2L);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(userService.findByEmail("bia@exemplo.test")).thenReturn(Optional.of(bia));
        when(teamService.findMemberByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.empty());
        when(inviteService.findPendingByTeamAndEmail(10L, "bia@exemplo.test")).thenReturn(List.of());
        when(inviteService.generateToken()).thenReturn("t");
        when(inviteService.create(any(TeamInvite.class), eq("t"))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(10L, 1L, dto("bia@exemplo.test", TeamRole.CREATOR));

        verify(inviteService).create(any(TeamInvite.class), eq("t"));
    }

    @Test
    void execute_shouldRejectDuplicatePendingInviteWith409() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(userService.findByEmail("carla@exemplo.test")).thenReturn(Optional.empty());
        when(inviteService.findPendingByTeamAndEmail(10L, "carla@exemplo.test"))
                .thenReturn(List.of(invite(LocalDateTime.now().plusDays(2))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("carla@exemplo.test", TeamRole.CREATOR)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(inviteService, never()).create(any(), any());
    }

    @Test
    void execute_shouldReplaceAnExpiredPendingInvite() {
        List<TeamInvite> expired = List.of(invite(LocalDateTime.now().minusDays(1)));
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(userService.findByEmail("carla@exemplo.test")).thenReturn(Optional.empty());
        when(inviteService.findPendingByTeamAndEmail(10L, "carla@exemplo.test")).thenReturn(expired);
        when(inviteService.generateToken()).thenReturn("t");
        when(inviteService.create(any(TeamInvite.class), eq("t"))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(10L, 1L, dto("carla@exemplo.test", TeamRole.CREATOR));

        verify(inviteService).deleteAll(expired);
        verify(inviteService).create(any(TeamInvite.class), eq("t"));
    }

    @Test
    void execute_shouldPropagateForbiddenWhenNotAdmin() {
        when(teamService.requireRole(10L, 2L, TeamRole.ADMIN))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 2L, dto("carla@exemplo.test", TeamRole.CREATOR)));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(inviteService, emailService, userService);
    }

    @Test
    void execute_shouldSendTheEmailOnlyAfterTheTransactionCommits() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        when(userService.findByEmail("carla@exemplo.test")).thenReturn(Optional.empty());
        when(inviteService.findPendingByTeamAndEmail(10L, "carla@exemplo.test")).thenReturn(List.of());
        when(inviteService.generateToken()).thenReturn("tok");
        when(inviteService.create(any(TeamInvite.class), eq("tok"))).thenAnswer(inv -> inv.getArgument(0));
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            useCase.execute(10L, 1L, dto("carla@exemplo.test", TeamRole.CREATOR));

            verifyNoInteractions(emailService);
            var synchronizations = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());
            synchronizations.get(0).afterCommit();
            verify(emailService).sendTeamInvite("carla@exemplo.test", "Ana Admin", "Equipe Office", "Criador", "tok");
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void execute_shouldStopWith402WhenMembersAndPendingInvitesFillThePlan() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "limite")).when(planService).requireCanInvite(admin.getTeam());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("carla@exemplo.test", TeamRole.CREATOR)));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
        // only the pending-invite lookup ran; nothing was deleted, created or e-mailed
        verify(inviteService).findPendingByTeamAndEmail(10L, "carla@exemplo.test");
        verifyNoMoreInteractions(inviteService);
        verifyNoInteractions(emailService);
    }

    @Test
    void execute_shouldReportAnOpenInviteAs409EvenWhenThePlanIsFull() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN)).thenReturn(admin);
        TeamInvite open = mock(TeamInvite.class);
        when(open.isOpen()).thenReturn(true);
        when(inviteService.findPendingByTeamAndEmail(10L, "carla@exemplo.test")).thenReturn(List.of(open));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, dto("carla@exemplo.test", TeamRole.CREATOR)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(planService, never()).requireCanInvite(any());
    }
}
