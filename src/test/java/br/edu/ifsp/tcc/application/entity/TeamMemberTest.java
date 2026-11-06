package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TeamMemberTest {

    @Test
    void onCreate_shouldSetJoinedAt() {
        TeamMember member = new TeamMember();

        member.onCreate();

        assertNotNull(member.getJoinedAt());
    }

    @Test
    void setters_shouldExposeValues() {
        Team team = new Team();
        User user = new User();
        TeamMember member = new TeamMember();
        member.setId(3L);
        member.setTeam(team);
        member.setUser(user);
        member.setRole(TeamRole.REVIEWER);

        assertEquals(3L, member.getId());
        assertSame(team, member.getTeam());
        assertSame(user, member.getUser());
        assertEquals(TeamRole.REVIEWER, member.getRole());
    }
}
