package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class M4EntitiesTest {

    private void prePersist(Object entity) throws Exception {
        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);
    }

    @Test
    void teamInvite_shouldBeOpenOnlyWhenPendingAndNotExpired() throws Exception {
        TeamInvite invite = new TeamInvite();
        Team team = new Team();
        User ana = new User();
        invite.setId(3L);
        invite.setTeam(team);
        invite.setEmail("carla@exemplo.test");
        invite.setRole(TeamRole.REVIEWER);
        invite.setTokenHash("h");
        invite.setInvitedBy(ana);
        invite.setExpiresAt(LocalDateTime.now().plusDays(1));
        prePersist(invite);

        assertEquals(3L, invite.getId());
        assertSame(team, invite.getTeam());
        assertEquals("carla@exemplo.test", invite.getEmail());
        assertEquals(TeamRole.REVIEWER, invite.getRole());
        assertEquals("h", invite.getTokenHash());
        assertSame(ana, invite.getInvitedBy());
        assertNotNull(invite.getCreatedAt());
        assertEquals(InviteStatus.PENDING, invite.getStatus());
        assertTrue(invite.isOpen());

        invite.setStatus(InviteStatus.ACCEPTED);
        assertFalse(invite.isOpen());

        invite.setStatus(InviteStatus.PENDING);
        invite.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertTrue(invite.isExpired());
        assertFalse(invite.isOpen());
    }

    @Test
    void emailChangeToken_shouldExpireAndKeepItsFields() throws Exception {
        EmailChangeToken token = new EmailChangeToken();
        User ana = new User();
        token.setId(1L);
        token.setUser(ana);
        token.setNewEmail("novo@exemplo.test");
        token.setToken("123456");
        token.setAttempts(2);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        prePersist(token);

        assertEquals(1L, token.getId());
        assertSame(ana, token.getUser());
        assertEquals("novo@exemplo.test", token.getNewEmail());
        assertEquals("123456", token.getToken());
        assertEquals(2, token.getAttempts());
        assertNotNull(token.getCreatedAt());
        assertFalse(token.isExpired());

        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        assertTrue(token.isExpired());
    }

    @Test
    void team_shouldNotBePersonalByDefault() {
        Team team = new Team();
        assertFalse(team.isPersonal());
        team.setPersonal(true);
        assertTrue(team.isPersonal());
    }
}
