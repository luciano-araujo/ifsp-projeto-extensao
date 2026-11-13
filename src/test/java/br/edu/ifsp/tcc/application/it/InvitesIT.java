package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.service.InviteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Contract 7.14: invites by e-mail (create, list, delete, public lookup, accept). */
@Import(MailCapture.Config.class)
class InvitesIT extends AbstractIT {

    private String email(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@creatorworkbench.test";
    }

    private String invitesPath(Team team) {
        return team.path() + "/invites";
    }

    private Resp invite(Team team, String email, String role) {
        return api.post(invitesPath(team), ana.token(), map("email", email, "role", role));
    }

    private String tokenSentTo(String email) {
        String token = MailCapture.inviteToken(email);
        assertNotNull(token, "an invite e-mail must have been sent to " + email);
        return token;
    }

    @Test
    @DisplayName("IT-INV-01 POST invite: 200 with the contract shape, +7 days, e-mail sent, only the SHA-256 hash stored")
    void createInvite() {
        Team team = newTeam();
        String email = email("carla");

        Resp created = invite(team, email, "REVIEWER");

        assertEquals(200, created.status(), created.body());
        assertEquals(email, created.text("email"));
        assertEquals("REVIEWER", created.text("role"));
        assertEquals("PENDING", created.text("status"));
        assertEquals(ana.id(), created.num("invitedBy.userId"));
        assertEquals("Ana Teste", created.text("invitedBy.name"));
        assertTrue(created.text("createdAt").endsWith("Z"));
        assertTrue(created.text("expiresAt").endsWith("Z"));
        long seconds = java.time.Duration.between(
                java.time.Instant.parse(created.text("createdAt")), java.time.Instant.parse(created.text("expiresAt"))).getSeconds();
        assertEquals(7 * 24 * 3600, seconds, 2);

        String token = tokenSentTo(email);
        String stored = jdbc.queryForObject("SELECT token_hash FROM team_invites WHERE id = ?", String.class, created.num("id"));
        assertEquals(InviteService.hash(token), stored);
        assertEquals(64, stored.length());
        assertNotEquals(token, stored);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM team_invites WHERE token_hash = ?", Integer.class, token));
    }

    @Test
    @DisplayName("IT-INV-02 Invite role defaults to CREATOR; invalid e-mail and role are 400")
    void validation() {
        Team team = newTeam();

        Resp defaulted = api.post(invitesPath(team), ana.token(), map("email", email("dflt")));
        assertEquals(200, defaulted.status(), defaulted.body());
        assertEquals("CREATOR", defaulted.text("role"));

        assertEquals(400, api.post(invitesPath(team), ana.token(), map("email", "not-an-email", "role", "CREATOR")).status());
        assertEquals(400, api.post(invitesPath(team), ana.token(), map("email", "", "role", "CREATOR")).status());
        assertEquals(400, api.post(invitesPath(team), ana.token(), map("email", email("x"), "role", "BOSS")).status());
    }

    @Test
    @DisplayName("IT-INV-03 Roles: create, list and delete are ADMIN only (403), outsiders get 403, no token 401")
    void roles() {
        Team team = newTeam();
        Resp created = invite(team, email("roles"), "CREATOR");
        long id = created.num("id");

        for (Person who : new Person[] {carlos, rui, bia, loner}) {
            assertEquals(403, api.post(invitesPath(team), who.token(), map("email", email("y"), "role", "CREATOR")).status());
            assertEquals(403, api.get(invitesPath(team), who.token()).status());
            assertEquals(403, api.delete(invitesPath(team) + "/" + id, who.token()).status());
        }
        assertEquals(401, api.get(invitesPath(team), null).status());
        assertEquals(1, api.get(invitesPath(team), ana.token()).size());
    }

    @Test
    @DisplayName("IT-INV-04 Duplicate pending invite is 409; invite for an existing member is 409")
    void conflicts() {
        Team team = newTeam();
        String email = email("dup");
        assertEquals(200, invite(team, email, "CREATOR").status());

        Resp duplicate = invite(team, email.toUpperCase(), "REVIEWER");
        assertEquals(409, duplicate.status());
        assertEquals("Já existe um convite pendente para este e-mail.", duplicate.message());

        Resp member = invite(team, carlos.email(), "CREATOR");
        assertEquals(409, member.status());
        assertEquals("Este usuário já faz parte da equipe.", member.message());
        assertEquals(1, api.get(invitesPath(team), ana.token()).size());
    }

    @Test
    @DisplayName("IT-INV-05 GET invites lists pending ones newest first, without the accepted and the expired")
    void list() {
        Team team = newTeam();
        // Plans: members + pending invites must fit the 5 seats of the fixture plan, so make room first
        jdbc.update("DELETE FROM team_members WHERE id IN (?, ?)", team.creatorMemberId(), team.reviewerMemberId());
        long first = invite(team, email("l1"), "CREATOR").num("id");
        long second = invite(team, email("l2"), "REVIEWER").num("id");
        Person dora = person("dora");
        long accepted = invite(team, dora.email(), "CREATOR").num("id");
        assertEquals(200, api.post("/api/v1/invites/" + tokenSentTo(dora.email()) + "/accept", dora.token(), null).status());
        long expired = invite(team, email("l3"), "CREATOR").num("id");
        jdbc.update("UPDATE team_invites SET expires_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 HOUR) WHERE id = ?", expired);

        Resp list = api.get(invitesPath(team), ana.token());

        assertEquals(200, list.status(), list.body());
        assertEquals(2, list.size());
        assertEquals(second, list.at("0.id").asLong());
        assertEquals(first, list.at("1.id").asLong());
        assertEquals("PENDING", list.at("0.status").asText());
        assertNotEquals(accepted, list.at("0.id").asLong());
    }

    @Test
    @DisplayName("IT-INV-06 Public lookup: no login needed, exposes only the contract fields; unknown token is 404")
    void publicLookup() {
        Team team = newTeam();
        String email = email("pub");
        invite(team, email, "REVIEWER");

        Resp info = api.get("/api/auth/invites/" + tokenSentTo(email), null);

        assertEquals(200, info.status(), info.body());
        assertEquals(email, info.text("email"));
        assertEquals("REVIEWER", info.text("role"));
        assertEquals("Ana Teste", info.text("invitedByName"));
        assertTrue(info.text("teamName").startsWith("Equipe "));
        assertTrue(info.text("expiresAt").endsWith("Z"));
        assertEquals(5, info.json().size(), "teamName, role, email, invitedByName, expiresAt");

        Resp unknown = api.get("/api/auth/invites/does-not-exist", null);
        assertEquals(404, unknown.status());
        assertEquals("Convite não encontrado ou expirado.", unknown.message());
    }

    @Test
    @DisplayName("IT-INV-07 Accept: the invited user joins with the invite role and gets the team; a second accept is 404")
    void accept() {
        Team team = newTeam();
        Person dora = person("dora");
        invite(team, dora.email(), "REVIEWER");
        String token = tokenSentTo(dora.email());

        Resp accepted = api.post("/api/v1/invites/" + token + "/accept", dora.token(), null);

        assertEquals(200, accepted.status(), accepted.body());
        assertEquals(team.id(), accepted.num("id"));
        assertEquals("REVIEWER", accepted.text("role"));
        assertEquals(4, accepted.num("membersCount"));
        assertTrue(accepted.text("createdAt").endsWith("Z"));
        // she can now read the team
        assertEquals(200, api.get(team.path() + "/members", dora.token()).status());
        assertEquals("ACCEPTED", jdbc.queryForObject("SELECT status FROM team_invites WHERE token_hash = ?", String.class,
                InviteService.hash(token)));

        assertEquals(404, api.post("/api/v1/invites/" + token + "/accept", dora.token(), null).status());
        assertEquals(404, api.get("/api/auth/invites/" + token, null).status());
    }

    @Test
    @DisplayName("IT-INV-08 Accept with another account is 403 and the invite stays usable; the e-mail match ignores case")
    void acceptWithWrongEmail() {
        Team team = newTeam();
        Person dora = person("dora");
        invite(team, dora.email().toUpperCase(), "CREATOR");
        String token = tokenSentTo(dora.email());

        Resp wrong = api.post("/api/v1/invites/" + token + "/accept", loner.token(), null);
        assertEquals(403, wrong.status());
        assertEquals("Este convite foi enviado para outro e-mail.", wrong.message());
        assertEquals(403, api.get(team.path() + "/members", loner.token()).status());
        assertEquals(401, api.post("/api/v1/invites/" + token + "/accept", null, null).status());

        assertEquals(200, api.post("/api/v1/invites/" + token + "/accept", dora.token(), null).status());
    }

    @Test
    @DisplayName("IT-INV-09 Accept by someone who already joined the team is 409")
    void acceptWhenAlreadyMember() {
        Team team = newTeam();
        Person dora = person("dora");
        invite(team, dora.email(), "CREATOR");
        addMemberDirect(team.id(), dora, "CREATOR");

        Resp r = api.post("/api/v1/invites/" + tokenSentTo(dora.email()) + "/accept", dora.token(), null);

        assertEquals(409, r.status());
        assertEquals("Você já faz parte desta equipe.", r.message());
    }

    @Test
    @DisplayName("IT-INV-10 Expired invite: lookup and accept are 404; the same e-mail can be invited again")
    void expired() {
        Team team = newTeam();
        Person dora = person("dora");
        long id = invite(team, dora.email(), "CREATOR").num("id");
        String oldToken = tokenSentTo(dora.email());
        jdbc.update("UPDATE team_invites SET expires_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 SECOND) WHERE id = ?", id);

        assertEquals(404, api.get("/api/auth/invites/" + oldToken, null).status());
        assertEquals(404, api.post("/api/v1/invites/" + oldToken + "/accept", dora.token(), null).status());

        MailCapture.INVITE_TOKENS.remove(dora.email().toLowerCase());
        Resp again = invite(team, dora.email(), "CREATOR");
        assertEquals(200, again.status(), again.body());
        assertNotEquals(id, again.num("id"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM team_invites WHERE id = ?", Integer.class, id));
        String newToken = tokenSentTo(dora.email());
        assertNotEquals(oldToken, newToken);
        assertEquals(200, api.post("/api/v1/invites/" + newToken + "/accept", dora.token(), null).status());
    }

    @Test
    @DisplayName("IT-INV-11 DELETE invite: 204 and the token stops working; an invite of another team is 404")
    void deleteInvite() {
        Team team = newTeam();
        String email = email("del");
        long id = invite(team, email, "CREATOR").num("id");
        String token = tokenSentTo(email);

        Resp otherTeamInvite = api.post(invitesPath(otherTeam), bia.token(), map("email", email("o"), "role", "CREATOR"));
        assertEquals(200, otherTeamInvite.status(), otherTeamInvite.body());
        assertEquals(404, api.delete(invitesPath(team) + "/" + otherTeamInvite.num("id"), ana.token()).status());

        assertEquals(204, api.delete(invitesPath(team) + "/" + id, ana.token()).status());
        assertEquals(404, api.get("/api/auth/invites/" + token, null).status());
        assertEquals(404, api.delete(invitesPath(team) + "/" + id, ana.token()).status());
        assertEquals(0, api.get(invitesPath(team), ana.token()).size());
    }

    // --- Decision 22: POST /teams/{t}/members is an invite ---

    @Test
    @DisplayName("IT-INV-20 POST members with an unregistered e-mail: 200 with an InviteResponse (no 404), e-mail sent")
    void addMemberWithUnknownEmailIsAnInvite() {
        Team team = newTeam();
        String unknown = email("ninguem");

        Resp r = api.post(team.path() + "/members", ana.token(), map("email", unknown, "role", "REVIEWER"));

        assertEquals(200, r.status(), r.body());
        assertEquals(unknown, r.text("email"));
        assertEquals("REVIEWER", r.text("role"));
        assertEquals("PENDING", r.text("status"));
        assertEquals(ana.id(), r.num("invitedBy.userId"));
        assertNotNull(tokenSentTo(unknown));
    }

    @Test
    @DisplayName("IT-INV-21 POST members with a registered e-mail: 200 with an invite and NOT a member until accept")
    void addMemberWithKnownEmailIsNotAMemberUntilAccept() {
        Team team = newTeam();
        Person dora = person("dora");
        int before = api.get(team.path() + "/members", ana.token()).size();

        Resp r = api.post(team.path() + "/members", ana.token(), map("email", dora.email(), "role", "CREATOR"));

        assertEquals(200, r.status(), r.body());
        assertEquals("PENDING", r.text("status"));
        assertEquals(before, api.get(team.path() + "/members", ana.token()).size());
        assertEquals(403, api.get(team.path() + "/members", dora.token()).status(), "not a member yet");

        assertEquals(200, api.post("/api/v1/invites/" + tokenSentTo(dora.email()) + "/accept", dora.token(), null).status());
        assertEquals(before + 1, api.get(team.path() + "/members", ana.token()).size());
    }

    @Test
    @DisplayName("IT-INV-22 POST members follows the invite rules: 409 for a member and for an open invite, 403 for non-admin")
    void addMemberFollowsTheInviteRules() {
        Team team = newTeam();
        Person dora = person("dora");

        assertEquals(409, api.post(team.path() + "/members", ana.token(), map("email", carlos.email(), "role", "CREATOR")).status());
        assertEquals(200, api.post(team.path() + "/members", ana.token(), map("email", dora.email(), "role", "CREATOR")).status());
        assertEquals(409, api.post(team.path() + "/members", ana.token(), map("email", dora.email(), "role", "CREATOR")).status());
        assertEquals(409, invite(team, dora.email(), "CREATOR").status(), "the open invite is shared by both routes");
        assertEquals(403, api.post(team.path() + "/members", carlos.token(), map("email", email("x"), "role", "CREATOR")).status());
    }
}
