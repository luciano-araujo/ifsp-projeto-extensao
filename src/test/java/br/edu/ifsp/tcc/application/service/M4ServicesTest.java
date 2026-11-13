package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.EmailChangeToken;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.EmailChangeTokenRepository;
import br.edu.ifsp.tcc.application.repository.TeamMemberRepository;
import br.edu.ifsp.tcc.application.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** What slice M4 added to TeamService, EmailService and the e-mail-change token service. */
@ExtendWith(MockitoExtension.class)
class M4ServicesTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private TagService tagService;
    @Mock private MaterialService materialService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private EmailChangeTokenRepository emailChangeTokenRepository;
    @Mock private JavaMailSender mailSender;

    private TeamService teamService() {
        return new TeamService(teamRepository, teamMemberRepository, boardColumnRepository, tagService,
                materialService, kanbanItemService, mock(PlanService.class));
    }

    // --- TeamService ---

    @Test
    void createPersonalTeam_shouldFlagTheTeamAsPersonalWithTheAutomaticName() {
        User ana = new User();
        ana.setName("Ana Souza");
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        TeamMember admin = teamService().createPersonalTeam(ana);

        assertTrue(admin.getTeam().isPersonal());
        assertEquals("Estúdio de Ana", admin.getTeam().getName());
        assertEquals(TeamRole.ADMIN, admin.getRole());
    }

    @Test
    void createTeamWithAdmin_shouldNotFlagTheTeamAsPersonal() {
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> inv.getArgument(0));

        assertFalse(teamService().createTeamWithAdmin(new User(), "Equipe").getTeam().isPersonal());
    }

    @Test
    void requireDeletable_shouldRefusePersonalTeamsWith409AndAcceptOthers() {
        Team personal = new Team();
        personal.setPersonal(true);
        TeamService service = teamService();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.requireDeletable(personal));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("A equipe pessoal não pode ser excluída.", ex.getReason());
        assertDoesNotThrow(() -> service.requireDeletable(new Team()));
    }

    @Test
    void renameTeam_shouldSetTheNameAndSave() {
        Team team = new Team();
        when(teamRepository.save(team)).thenReturn(team);

        Team renamed = teamService().renameTeam(team, "Office Resolve Cursos");

        assertEquals("Office Resolve Cursos", renamed.getName());
    }

    @Test
    void findMembersOfTeams_shouldLoadAllMembersWithTheirUsersInOneQuery() {
        List<TeamMember> members = List.of(new TeamMember());
        when(teamMemberRepository.findByTeamIdsWithUser(List.of(10L, 20L))).thenReturn(members);

        assertSame(members, teamService().findMembersOfTeams(List.of(10L, 20L)));
    }

    // --- EmailChangeTokenService ---

    @Test
    void emailChangeTokenService_shouldDelegateToTheRepository() {
        EmailChangeTokenService service = new EmailChangeTokenService(emailChangeTokenRepository);
        EmailChangeToken token = new EmailChangeToken();
        when(emailChangeTokenRepository.save(token)).thenReturn(token);
        when(emailChangeTokenRepository.findByUser_Id(1L)).thenReturn(Optional.of(token));
        LocalDateTime now = LocalDateTime.now();
        when(emailChangeTokenRepository.deleteExpired(now)).thenReturn(3);

        assertSame(token, service.save(token));
        assertSame(token, service.findByUserId(1L).orElseThrow());
        service.deleteByUserId(1L);
        verify(emailChangeTokenRepository).deleteByUserId(1L);
        assertEquals(3, service.deleteExpired(now));
    }

    // --- EmailService ---

    private EmailService emailService(String baseUrl) {
        EmailService service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", baseUrl);
        return service;
    }

    private SimpleMailMessage sent() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void sendEmailChangeToken_shouldMentionTheCodeAndTheTenMinutes() {
        emailService("http://x").sendEmailChangeToken("novo@exemplo.test", "Ana", "482917");

        SimpleMailMessage message = sent();
        assertEquals("novo@exemplo.test", message.getTo()[0]);
        assertTrue(message.getText().contains("482917"));
        assertTrue(message.getText().contains("10 minutos"));
    }

    @Test
    void sendTeamInvite_shouldLinkToTheConvitePageOfTheFrontend() {
        emailService("https://app.exemplo.test/").sendTeamInvite("carla@exemplo.test", "Ana", "Equipe Office",
                "Criador", "tok-123");

        SimpleMailMessage message = sent();
        assertTrue(message.getSubject().contains("Equipe Office"));
        assertTrue(message.getText().contains("https://app.exemplo.test/convite?token=tok-123"));
        assertTrue(message.getText().contains("Ana convidou"));
        assertTrue(message.getText().contains("Criador"));
        assertTrue(message.getText().contains("7 dias"));
    }

    @Test
    void sendTeamInvite_shouldWorkWhenTheInviterIsUnknown() {
        emailService("http://localhost:3000").sendTeamInvite("carla@exemplo.test", null, "Equipe", "Revisor", "t");

        assertTrue(sent().getText().startsWith("Você convidou"));
    }

    @Test
    void sendDeadlineDigest_shouldListEveryLine() {
        emailService("http://localhost:3000").sendDeadlineDigest("ana@exemplo.test", "Ana", List.of("linha 1", "linha 2"));

        SimpleMailMessage message = sent();
        assertTrue(message.getText().contains("linha 1\nlinha 2"));
        assertTrue(message.getSubject().contains("Prazos"));
    }

    @Test
    void emailChangeRegisterAttempt_shouldReturnTheNewCounterOrMinusOneAtTheCap() {
        EmailChangeTokenService service = new EmailChangeTokenService(emailChangeTokenRepository);
        when(emailChangeTokenRepository.incrementAttempts(3L, 5)).thenReturn(1, 0);
        when(emailChangeTokenRepository.findAttempts(3L)).thenReturn(2);

        assertEquals(2, service.registerAttempt(3L, 5));
        assertEquals(-1, service.registerAttempt(3L, 5));
    }

    @Test
    void lockTeam_shouldTakeTheRowLock() {
        teamService().lockTeam(10L);

        verify(teamRepository).findByIdForUpdate(10L);
    }
}
