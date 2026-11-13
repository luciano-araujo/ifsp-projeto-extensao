package br.edu.ifsp.tcc.application.it;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Security slice A: session invalidation, password reset by stored e-mail, script size, control characters, last admin. */
class SecuritySliceAIT extends AbstractIT {

    private String token(Person p, Integer tokenVersion) {
        var builder = JWT.create().withIssuer("creator-workbench").withSubject(String.valueOf(p.id()))
                .withIssuedAt(Instant.now().minusSeconds(60)).withExpiresAt(Instant.now().plusSeconds(600));
        if (tokenVersion != null) builder.withClaim("tv", tokenVersion);
        return builder.sign(Algorithm.HMAC256(environment.getProperty("app.jwt.secret")));
    }

    @Test
    @DisplayName("IT-SECA-01 Token without tv or with another version is 401; the right version works")
    void tokenVersionIsRequired() {
        Person user = person("tv");
        String path = "/api/v1/users/" + user.id();
        assertEquals(200, api.get(path, token(user, 0)).status());
        assertEquals(401, api.get(path, token(user, null)).status());
        assertEquals(401, api.get(path, token(user, 1)).status());
        assertEquals(401, api.get(path, token(user, -1)).status());
    }

    @Test
    @DisplayName("IT-SECA-02 Password reset (e-mail typed in other case): the code is keyed to the stored address and every older session dies")
    void resetInvalidatesSessions() {
        Person user = person("rst");
        String typed = user.email().toUpperCase();

        assertEquals(200, api.post("/api/auth/password/forgot", null, map("email", typed)).status());
        String code = jdbc.queryForObject("SELECT token FROM password_reset_tokens WHERE email = ?", String.class, user.email());
        assertNotNull(code, "the code is keyed to the stored e-mail");
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE BINARY email = ?", Integer.class, user.email()),
                "stored in the stored spelling, not in the typed one");

        Resp reset = api.post("/api/auth/password/reset", null, map("email", typed, "token", code, "newPassword", "NovaSenha@456"));
        assertEquals(200, reset.status(), reset.body());

        assertEquals(401, api.get("/api/v1/users/" + user.id(), user.token()).status());
        Resp login = api.post("/api/auth/login", null, map("email", user.email(), "password", "NovaSenha@456"));
        assertEquals(200, login.status(), login.body());
        assertEquals(200, api.get("/api/v1/users/" + user.id(), login.text("token")).status());
    }

    @Test
    @DisplayName("IT-SECA-03 Script content over 500000 characters is 400; exactly 500000 is saved")
    void scriptSizeLimit() {
        Team team = newTeam();
        long card = projectId(team, "Roteiro grande");
        String path = "/api/v1/kanban/" + card + "/script";

        Resp tooBig = api.put(path, ana.token(), map("content", "a".repeat(500_001)));
        assertEquals(400, tooBig.status());
        assertEquals("O roteiro deve ter no máximo 500.000 caracteres.", tooBig.message());
        assertEquals(200, api.put(path, ana.token(), map("content", "a".repeat(500_000))).status());
    }

    @Test
    @DisplayName("IT-SECA-04 User name with a line break or control character is 400 on PUT /users/{id} and on sign-up")
    void controlCharactersInNames() {
        Person user = person("ctl");
        String path = "/api/v1/users/" + user.id();
        for (String bad : new String[]{"Ana\nSilva", "Ana\tSilva", "Ana\u0000"}) {
            Resp r = api.put(path, user.token(), map("name", bad));
            assertEquals(400, r.status(), bad);
            assertEquals("O texto não pode conter quebras de linha ou caracteres de controle.", r.message());
        }
        assertEquals(200, api.put(path, user.token(), map("name", "José da Silva")).status());
        Resp signUp = api.post("/api/auth/register/request-token", null,
                map("name", "Ana\nSilva", "email", "novo." + System.nanoTime() + "@creatorworkbench.test"));
        assertEquals(400, signUp.status(), signUp.body());
    }

    @Test
    @DisplayName("IT-SECA-05 Two admins demoting each other at the same time leave at least one ADMIN (repeated)")
    void lastAdminRace() throws Exception {
        for (int round = 0; round < 5; round++) {
            Team team = newTeam();
            assertEquals(200, api.patch(team.path() + "/members/" + team.creatorMemberId(), ana.token(), map("role", "ADMIN")).status());
            List<Callable<Resp>> calls = List.of(
                    () -> api.patch(team.path() + "/members/" + team.creatorMemberId(), ana.token(), map("role", "CREATOR")),
                    () -> api.patch(team.path() + "/members/" + team.adminMemberId(), carlos.token(), map("role", "CREATOR")));
            List<Resp> results = Parallel.run(calls);

            int admins = jdbc.queryForObject("SELECT COUNT(*) FROM team_members WHERE team_id = ? AND role = 'ADMIN'", Integer.class, team.id());
            assertEquals(1, admins, "round " + round + ": " + results.stream().map(r -> r.status() + " " + r.body()).toList());
        }
    }

    @Test
    @DisplayName("IT-SECA-06 Two admins leaving at the same time leave one ADMIN (repeated)")
    void lastAdminLeaveRace() throws Exception {
        for (int round = 0; round < 5; round++) {
            Team team = newTeam();
            assertEquals(200, api.patch(team.path() + "/members/" + team.creatorMemberId(), ana.token(), map("role", "ADMIN")).status());
            List<Callable<Resp>> calls = List.of(
                    () -> api.delete(team.path() + "/members/" + team.adminMemberId(), ana.token()),
                    () -> api.delete(team.path() + "/members/" + team.creatorMemberId(), carlos.token()));
            List<Resp> results = Parallel.run(calls);

            int admins = jdbc.queryForObject("SELECT COUNT(*) FROM team_members WHERE team_id = ? AND role = 'ADMIN'", Integer.class, team.id());
            assertEquals(1, admins, "round " + round + ": " + results.stream().map(r -> r.status() + " " + r.body()).toList());
        }
    }
}
