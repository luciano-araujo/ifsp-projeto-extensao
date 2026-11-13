package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.dto.PublicInviteResponse;
import br.edu.ifsp.tcc.application.entity.InviteStatus;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamInvite;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.TeamInviteRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock private TeamInviteRepository teamInviteRepository;
    @InjectMocks private InviteService inviteService;

    private TeamInvite invite(InviteStatus status, LocalDateTime expiresAt) {
        Team team = new Team();
        team.setId(10L);
        team.setName("Equipe Office");
        User ana = new User();
        ana.setId(1L);
        ana.setName("Ana Admin");
        TeamInvite invite = new TeamInvite();
        invite.setId(3L);
        invite.setTeam(team);
        invite.setEmail("carla@exemplo.test");
        invite.setRole(TeamRole.CREATOR);
        invite.setStatus(status);
        invite.setInvitedBy(ana);
        invite.setExpiresAt(expiresAt);
        return invite;
    }

    @Test
    void hash_shouldBeTheKnownSha256HexOfTheToken() {
        // sha256("abc")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", InviteService.hash("abc"));
    }

    @Test
    void generateToken_shouldBeUrlSafeLongAndDifferentEachTime() {
        String first = inviteService.generateToken();
        String second = inviteService.generateToken();

        assertEquals(43, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
        assertNotEquals(first, second);
    }

    @Test
    void create_shouldStoreOnlyTheHashAndExpireInSevenDays() {
        when(teamInviteRepository.save(any(TeamInvite.class))).thenAnswer(inv -> inv.getArgument(0));
        TeamInvite invite = new TeamInvite();

        TeamInvite saved = inviteService.create(invite, "abc");

        assertEquals(InviteService.hash("abc"), saved.getTokenHash());
        assertNotEquals("abc", saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusDays(7).minusMinutes(1)));
        assertTrue(saved.getExpiresAt().isBefore(LocalDateTime.now().plusDays(7).plusMinutes(1)));
    }

    @Test
    void findOpenByToken_shouldLookUpByHashAndReturnOpenInvites() {
        TeamInvite open = invite(InviteStatus.PENDING, LocalDateTime.now().plusDays(1));
        when(teamInviteRepository.findByTokenHash(InviteService.hash("tok"))).thenReturn(Optional.of(open));

        assertSame(open, inviteService.findOpenByToken("tok"));
    }

    @Test
    void findOpenByToken_shouldBe404ForExpiredAcceptedAndUnknown() {
        when(teamInviteRepository.findByTokenHash(InviteService.hash("expired")))
                .thenReturn(Optional.of(invite(InviteStatus.PENDING, LocalDateTime.now().minusSeconds(1))));
        when(teamInviteRepository.findByTokenHash(InviteService.hash("used")))
                .thenReturn(Optional.of(invite(InviteStatus.ACCEPTED, LocalDateTime.now().plusDays(1))));
        when(teamInviteRepository.findByTokenHash(InviteService.hash("none"))).thenReturn(Optional.empty());

        for (String token : List.of("expired", "used", "none")) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> inviteService.findOpenByToken(token));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Convite não encontrado ou expirado.", ex.getReason());
        }
    }

    @Test
    void findOfTeam_shouldBe404WhenTheInviteIsOfAnotherTeam() {
        when(teamInviteRepository.findByIdAndTeam_Id(3L, 99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> inviteService.findOfTeam(99L, 3L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void findOfTeam_shouldReturnTheInvite() {
        TeamInvite found = invite(InviteStatus.PENDING, LocalDateTime.now().plusDays(1));
        when(teamInviteRepository.findByIdAndTeam_Id(3L, 10L)).thenReturn(Optional.of(found));

        assertSame(found, inviteService.findOfTeam(10L, 3L));
    }

    @Test
    void markAccepted_shouldChangeTheStatusAndSave() {
        TeamInvite found = invite(InviteStatus.PENDING, LocalDateTime.now().plusDays(1));

        inviteService.markAccepted(found);

        assertEquals(InviteStatus.ACCEPTED, found.getStatus());
        verify(teamInviteRepository).save(found);
    }

    @Test
    void finders_shouldDelegateToTheRepository() {
        List<TeamInvite> invites = List.of(invite(InviteStatus.PENDING, LocalDateTime.now().plusDays(1)));
        when(teamInviteRepository.findByTeam_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
                eq(10L), eq(InviteStatus.PENDING), any(LocalDateTime.class))).thenReturn(invites);
        when(teamInviteRepository.findByTeam_IdAndEmailIgnoreCaseAndStatus(10L, "a@b.test", InviteStatus.PENDING))
                .thenReturn(invites);
        when(teamInviteRepository.deleteExpired(any(LocalDateTime.class))).thenReturn(2);

        assertSame(invites, inviteService.findOpenByTeamId(10L));
        assertSame(invites, inviteService.findPendingByTeamAndEmail(10L, "a@b.test"));
        assertEquals(2, inviteService.deleteExpired(LocalDateTime.now()));
        inviteService.deleteAll(invites);
        inviteService.delete(invites.get(0));
        verify(teamInviteRepository).deleteAll(invites);
        verify(teamInviteRepository).delete(invites.get(0));
    }



    @Test
    void toInviteResponse_shouldShapeTheContract() {
        LocalDateTime expires = LocalDateTime.of(2026, 11, 20, 10, 0);
        TeamInvite found = invite(InviteStatus.PENDING, expires);

        InviteResponse response = inviteService.toInviteResponse(found);

        assertEquals(3L, response.getId());
        assertEquals("carla@exemplo.test", response.getEmail());
        assertEquals(TeamRole.CREATOR, response.getRole());
        assertEquals(InviteStatus.PENDING, response.getStatus());
        assertEquals(1L, response.getInvitedBy().getUserId());
        assertEquals("Ana Admin", response.getInvitedBy().getName());
        assertEquals(expires, response.getExpiresAt());
    }

    @Test
    void toInviteResponse_shouldHaveNullInviterWhenTheAccountWasDeleted() {
        TeamInvite found = invite(InviteStatus.PENDING, LocalDateTime.now().plusDays(1));
        found.setInvitedBy(null);

        assertNull(inviteService.toInviteResponse(found).getInvitedBy());
        assertNull(inviteService.toPublicResponse(found).getInvitedByName());
    }

    @Test
    void toPublicResponse_shouldExposeOnlyThePublicFields() {
        LocalDateTime expires = LocalDateTime.of(2026, 11, 20, 10, 0);

        PublicInviteResponse response = inviteService.toPublicResponse(invite(InviteStatus.PENDING, expires));

        assertEquals("Equipe Office", response.getTeamName());
        assertEquals(TeamRole.CREATOR, response.getRole());
        assertEquals("carla@exemplo.test", response.getEmail());
        assertEquals("Ana Admin", response.getInvitedByName());
        assertEquals(expires, response.getExpiresAt());
    }
}
