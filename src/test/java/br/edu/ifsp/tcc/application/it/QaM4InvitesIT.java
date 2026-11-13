package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.InviteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QA M4, hunting pass on invites (contract 7.14, SEC-22): enumeration through the public lookup, e-mail case,
 * double accept and double create under concurrency, cross-team ids, role bypass and header injection.
 */
@Import(MailCapture.Config.class)
class QaM4InvitesIT extends AbstractIT {

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
    @DisplayName("IT-INV-12 / SEC-22 Public lookup does not leak: unknown, accepted, cancelled and expired tokens answer the same 404")
    void lookupDoesNotLeakTokenState() {
        Team team = newTeam();
        Person dora = person("dora");

        invite(team, dora.email(), "CREATOR");
        String acceptedToken = tokenSentTo(dora.email());
        assertEquals(200, api.post("/api/v1/invites/" + acceptedToken + "/accept", dora.token(), null).status());

        String cancelledEmail = email("cancel");
        long cancelledId = invite(team, cancelledEmail, "CREATOR").num("id");
        String cancelledToken = tokenSentTo(cancelledEmail);
        assertEquals(204, api.delete(invitesPath(team) + "/" + cancelledId, ana.token()).status());

        String expiredEmail = email("old");
        long expiredId = invite(team, expiredEmail, "CREATOR").num("id");
        String expiredToken = tokenSentTo(expiredEmail);
        jdbc.update("UPDATE team_invites SET expires_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 SECOND) WHERE id = ?", expiredId);

        String liveEmail = email("live");
        invite(team, liveEmail, "CREATOR");
        String liveToken = tokenSentTo(liveEmail);

        Resp unknown = api.get("/api/auth/invites/" + "A".repeat(43), null);
        assertEquals(404, unknown.status());
        for (String token : new String[] {acceptedToken, cancelledToken, expiredToken}) {
            Resp r = api.get("/api/auth/invites/" + token, null);
            assertEquals(404, r.status());
            assertEquals(unknown.body(), r.body(), "same body for every dead token");
            assertEquals(unknown.header("content-type"), r.header("content-type"));
            // accept as a logged-in stranger: also the same answer, the state of the token is not revealed
            Resp accept = api.post("/api/v1/invites/" + token + "/accept", loner.token(), null);
            assertEquals(404, accept.status());
            assertEquals(unknown.body(), accept.body());
        }
        assertEquals(200, api.get("/api/auth/invites/" + liveToken, null).status());
    }

    @Test
    @DisplayName("IT-INV-13 / SEC-22 Malformed and hostile tokens are a clean 404, and the stored hash is not a usable token")
    void hostileTokens() {
        Team team = newTeam();
        String email = email("hash");
        invite(team, email, "CREATOR");
        String token = tokenSentTo(email);
        String hash = InviteService.hash(token);

        List<String> tokens = List.of(hash, token.toUpperCase().equals(token) ? token.toLowerCase() : token.toUpperCase(),
                "x".repeat(4000), "%27%20OR%20%271%27%3D%271", "a%20b", "%C3%A7%C3%A3o", "0", "null", "-1");
        for (String candidate : tokens) {
            Resp lookup = api.get("/api/auth/invites/" + candidate, null);
            assertEquals(404, lookup.status(), "lookup of " + candidate.substring(0, Math.min(20, candidate.length())) + ": " + lookup.body());
            Resp accept = api.post("/api/v1/invites/" + candidate + "/accept", loner.token(), null);
            assertEquals(404, accept.status(), "accept of " + candidate.substring(0, Math.min(20, candidate.length())) + ": " + accept.body());
        }
        // an encoded slash never reaches the application: Tomcat answers 400 itself (not a 5xx, nothing leaks)
        assertEquals(400, api.get("/api/auth/invites/..%2F..%2Fetc", null).status());
        assertEquals(400, api.get("/api/auth/invites/a%00b", null).status());
        // the invite is still usable with the real token
        assertEquals(200, api.get("/api/auth/invites/" + token, null).status());
    }

    @Test
    @DisplayName("IT-INV-14 Accepting with an e-mail written in another case works both ways (stored mixed case, invite in lower case)")
    void acceptWithDifferentCase() {
        Team team = newTeam();
        String mixed = "Mixed.Case." + UUID.randomUUID().toString().substring(0, 8) + "@CreatorWorkbench.test";
        User user = new User();
        user.setEmail(mixed);
        user.setName("Mixed Case");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user = userRepository.save(user);
        Resp login = api.post("/api/auth/login", null, map("email", mixed.toLowerCase(), "password", PASSWORD));
        assertEquals(200, login.status(), "login ignores the case of the e-mail: " + login.body());
        String jwt = login.text("token");

        assertEquals(200, invite(team, mixed.toLowerCase(), "REVIEWER").status());
        String token = tokenSentTo(mixed.toLowerCase());
        Resp accepted = api.post("/api/v1/invites/" + token + "/accept", jwt, null);
        assertEquals(200, accepted.status(), accepted.body());
        assertEquals("REVIEWER", accepted.text("role"));

        // now a member: inviting any spelling of the address is a 409, not a second pending invite
        assertEquals(409, invite(team, mixed.toUpperCase(), "CREATOR").status());
        assertEquals(409, invite(team, mixed, "CREATOR").status());
    }

    @Test
    @DisplayName("IT-INV-15 Accepting the same invite twice at the same time: one membership, no 5xx")
    void acceptTwiceAtTheSameTime() throws Exception {
        Team team = newTeam();
        Person dora = person("dora");
        invite(team, dora.email(), "CREATOR");
        String token = tokenSentTo(dora.email());

        List<Callable<Resp>> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) calls.add(() -> api.post("/api/v1/invites/" + token + "/accept", dora.token(), null));
        List<Resp> results = Parallel.run(calls);

        long ok = results.stream().filter(r -> r.status() == 200).count();
        assertTrue(ok >= 1, "at least one accept wins: " + results.stream().map(Resp::status).toList());
        assertTrue(results.stream().noneMatch(r -> r.status() >= 500),
                "no 5xx: " + results.stream().map(r -> r.status() + " " + r.body()).toList());
        assertTrue(results.stream().allMatch(r -> r.status() == 200 || r.status() == 404 || r.status() == 409),
                results.stream().map(Resp::status).toList().toString());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM team_members WHERE team_id = ? AND user_id = ?",
                Integer.class, team.id(), dora.id()));
    }

    @Test
    @DisplayName("IT-INV-16 Two invites to the same e-mail at the same time: only one stays pending (409 for the other)")
    void createTwiceAtTheSameTime() throws Exception {
        Team team = newTeam();
        String target = email("race");

        List<Callable<Resp>> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) calls.add(() -> invite(team, target, "CREATOR"));
        List<Resp> results = Parallel.run(calls);

        assertTrue(results.stream().noneMatch(r -> r.status() >= 500),
                "no 5xx: " + results.stream().map(r -> r.status() + " " + r.body()).toList());
        int pending = jdbc.queryForObject("SELECT COUNT(*) FROM team_invites WHERE team_id = ? AND email = ? AND status = 'PENDING'",
                Integer.class, team.id(), target);
        assertEquals(1, pending, "one pending invite per team and e-mail; statuses: " + results.stream().map(Resp::status).toList());
        assertEquals(1, results.stream().filter(r -> r.status() == 200).count());
    }

    @Test
    @DisplayName("IT-INV-17 A member who was removed cannot reuse an accepted invite; an admin can invite the address again")
    void removedMemberCannotReuseInvite() {
        Team team = newTeam();
        Person dora = person("dora");
        invite(team, dora.email(), "CREATOR");
        String token = tokenSentTo(dora.email());
        Resp accepted = api.post("/api/v1/invites/" + token + "/accept", dora.token(), null);
        assertEquals(200, accepted.status(), accepted.body());
        Resp members = api.get(team.path() + "/members", ana.token());
        long doraMember = 0;
        for (tools.jackson.databind.JsonNode m : members.json()) {
            if (dora.email().equals(m.path("email").asString())) doraMember = m.path("id").asLong();
        }
        assertTrue(doraMember > 0);
        assertEquals(204, api.delete(team.path() + "/members/" + doraMember, ana.token()).status());

        assertEquals(404, api.post("/api/v1/invites/" + token + "/accept", dora.token(), null).status(), "the token was consumed");
        MailCapture.INVITE_TOKENS.remove(dora.email().toLowerCase());
        Resp again = invite(team, dora.email(), "REVIEWER");
        assertEquals(200, again.status(), again.body());
        Resp second = api.post("/api/v1/invites/" + tokenSentTo(dora.email()) + "/accept", dora.token(), null);
        assertEquals(200, second.status(), second.body());
        assertEquals("REVIEWER", second.text("role"));
    }

    @Test
    @DisplayName("IT-INV-18 Cross-team: the admin of team A cannot list, create or cancel invites of team B; ids are checked against the path team")
    void crossTeamInvites() {
        Team team = newTeam();
        long mine = invite(team, email("mine"), "CREATOR").num("id");
        Resp theirs = api.post(invitesPath(otherTeam), bia.token(), map("email", email("theirs"), "role", "CREATOR"));
        assertEquals(200, theirs.status(), theirs.body());
        long theirId = theirs.num("id");

        // Ana is not a member of team B
        assertEquals(403, api.get(invitesPath(otherTeam), ana.token()).status());
        assertEquals(403, api.post(invitesPath(otherTeam), ana.token(), map("email", email("z"), "role", "CREATOR")).status());
        assertEquals(403, api.delete(invitesPath(otherTeam) + "/" + theirId, ana.token()).status());
        // her own path with the id of the other team's invite, and the reverse
        assertEquals(404, api.delete(invitesPath(team) + "/" + theirId, ana.token()).status());
        assertEquals(403, api.delete(invitesPath(otherTeam) + "/" + mine, ana.token()).status());
        assertEquals(404, api.delete(invitesPath(otherTeam) + "/" + mine, bia.token()).status());
        assertEquals(404, api.delete(invitesPath(team) + "/0", ana.token()).status());
        assertEquals(404, api.delete(invitesPath(team) + "/-5", ana.token()).status());
        assertEquals(400, api.delete(invitesPath(team) + "/abc", ana.token()).status());
        // nothing was removed
        assertEquals(1, api.get(invitesPath(team), ana.token()).size());
        assertEquals(1, api.get(invitesPath(otherTeam), bia.token()).size());
    }

    @Test
    @DisplayName("IT-INV-19 Role bypass: an invited CREATOR cannot raise the role with a body, and a CREATOR/REVIEWER member cannot manage invites")
    void roleBypass() {
        Team team = newTeam();
        Person dora = person("dora");
        invite(team, dora.email(), "CREATOR");
        String token = tokenSentTo(dora.email());

        Resp accepted = api.post("/api/v1/invites/" + token + "/accept", dora.token(), map("role", "ADMIN", "teamId", otherTeam.id()));
        assertEquals(200, accepted.status(), accepted.body());
        assertEquals("CREATOR", accepted.text("role"));
        assertEquals(team.id(), accepted.num("id"));

        // dora (creator now) and rui (reviewer) cannot manage invites or the team
        for (Person who : new Person[] {dora, rui, carlos}) {
            assertEquals(403, api.post(invitesPath(team), who.token(), map("email", email("x"), "role", "ADMIN")).status());
            assertEquals(403, api.get(invitesPath(team), who.token()).status());
            assertEquals(403, api.patch("/api/v1/teams/" + team.id(), who.token(), map("name", "Tomada")).status());
            assertEquals(403, api.delete("/api/v1/teams/" + team.id(), who.token()).status());
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM teams WHERE id = ? AND name <> 'Tomada'", Integer.class, team.id()));
    }

    @Test
    @DisplayName("IT-INV-20 Invalid e-mails (CR/LF, spaces, 256 chars, JSON null) are 400 and send nothing")
    void invalidEmails() {
        Team team = newTeam();
        String[] bad = {
                "a@creatorworkbench.test\r\nBcc: attacker@creatorworkbench.test",
                "a@creatorworkbench.test, b@creatorworkbench.test",
                " ",
                "a b@creatorworkbench.test",
                "x".repeat(250) + "@creatorworkbench.test"
        };
        for (String value : bad) {
            Resp r = api.post(invitesPath(team), ana.token(), map("email", value, "role", "CREATOR"));
            assertEquals(400, r.status(), "email [" + value.replace("\r", "\\r").replace("\n", "\\n") + "]: " + r.body());
        }
        assertEquals(400, api.post(invitesPath(team), ana.token(), "{\"email\":null}").status());
        assertEquals(400, api.post(invitesPath(team), ana.token(), "{}").status());
        assertEquals(400, api.post(invitesPath(team), ana.token(), "not json").status());
        assertEquals(0, api.get(invitesPath(team), ana.token()).size());
    }

    @Test
    @DisplayName("IT-INV-21 When the inviter deletes the account the invite stays valid with invitedBy null")
    void inviterDeletesAccount() {
        Person boss = person("boss");
        Team team = newTeamFor(boss, "Equipe da chefe", ana, null);
        // ana becomes the second admin so that the boss may leave
        long anaMember = team.creatorMemberId();
        assertEquals(200, api.patch(team.path() + "/members/" + anaMember, boss.token(), map("role", "ADMIN")).status());
        String target = email("heir");
        Resp created = api.post(invitesPath(team), boss.token(), map("email", target, "role", "CREATOR"));
        assertEquals(200, created.status(), created.body());
        String token = tokenSentTo(target);

        assertEquals(204, api.delete("/api/v1/users/" + boss.id(), boss.token()).status());

        Resp lookup = api.get("/api/auth/invites/" + token, null);
        assertEquals(200, lookup.status(), lookup.body());
        assertNull(lookup.text("invitedByName"));
        Resp list = api.get(invitesPath(team), ana.token());
        assertEquals(200, list.status(), list.body());
        assertEquals(1, list.size());
        assertTrue(list.at("0.invitedBy").isNull() || list.at("0.invitedBy").isMissingNode(), list.body());
    }
}
