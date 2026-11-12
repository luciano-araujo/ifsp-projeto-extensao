package br.edu.ifsp.tcc.application.it;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** SEC-01..SEC-14 of the plan against the real filter chain, plus the 401 matrix over the M1 routes. */
class SecurityIT extends AbstractIT {

    private String forge(String secret, long userId, Instant expiresAt) {
        return JWT.create().withIssuer("creator-workbench").withSubject(String.valueOf(userId))
                .withIssuedAt(Instant.now().minusSeconds(3600)).withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(secret));
    }

    @Test
    @DisplayName("SEC-01 JWT with invalid signature is rejected (401); same token signed with the right secret works")
    void invalidSignature() {
        String secret = environment.getProperty("app.jwt.secret");
        Resp good = api.get("/api/v1/teams", forge(secret, ana.id(), Instant.now().plusSeconds(600)));
        assertEquals(200, good.status(), "sanity: a forged-but-valid token is accepted, so the test really checks the signature");
        assertEquals(401, api.get("/api/v1/teams", forge("another-secret-another-secret-another", ana.id(), Instant.now().plusSeconds(600))).status());
        String valid = ana.token();
        String tampered = valid.substring(0, valid.length() - 3) + (valid.endsWith("AAA") ? "BBB" : "AAA");
        assertEquals(401, api.get("/api/v1/teams", tampered).status());
        // tampering with the payload (another user id) while keeping the signature
        String[] parts = valid.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        String swapped = payload.replace("\"sub\":\"" + ana.id() + "\"", "\"sub\":\"" + bia.id() + "\"");
        assertNotEquals(payload, swapped);
        String forgedPayload = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(swapped.getBytes()) + "." + parts[2];
        assertEquals(401, api.get("/api/v1/teams", forgedPayload).status());
    }

    @Test
    @DisplayName("SEC-02 / SEC-13 Expired JWT is rejected (401)")
    void expired() {
        String secret = environment.getProperty("app.jwt.secret");
        assertEquals(401, api.get("/api/v1/teams", forge(secret, ana.id(), Instant.now().minusSeconds(5))).status());
        assertEquals(24 * 3600 * 1000L, Long.parseLong(environment.getProperty("app.jwt.expiration-ms", "86400000")));
    }

    @Test
    @DisplayName("SEC-01b Garbage, alg=none, wrong scheme, empty bearer and token of a deleted user are all 401")
    void malformedTokens() {
        String none = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes()) + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"iss\":\"creator-workbench\",\"sub\":\"" + ana.id() + "\"}").getBytes()) + ".";
        for (String token : List.of("garbage", "a.b.c", none, "", "null", "Bearer")) {
            assertEquals(401, api.get("/api/v1/teams", token).status(), "token '" + token + "'");
        }
        assertEquals(401, api.send("GET", "/api/v1/teams", null, null, Map.of("Authorization", "Basic YW5hOnRlc3Q=")).status());
        assertEquals(401, api.send("GET", "/api/v1/teams", null, null, Map.of("Authorization", ana.token())).status(), "missing Bearer prefix");

        Person ghost = person("ghost");
        assertEquals(200, api.get("/api/v1/teams", ghost.token()).status());
        jdbc.update("DELETE FROM team_members WHERE user_id = ?", ghost.id());
        jdbc.update("DELETE FROM users WHERE id = ?", ghost.id());
        assertEquals(401, api.get("/api/v1/teams", ghost.token()).status(), "valid signature but the user no longer exists");
    }

    @Test
    @DisplayName("SEC-03 / IT-03 No token: every M1 route answers 401 (never 200/403/404/500)")
    void noTokenMatrix() {
        Team team = newTeam();
        long card = projectId(team, "401");
        long task = taskId(team, card, "t");
        String t = team.path();
        Object body = map("title", "x", "name", "x", "color", "#112233", "columnIds", List.of(1), "taskIds", List.of(1), "content", "x");
        List<String[]> routes = List.of(
                new String[]{"GET", t + "/board"}, new String[]{"GET", t + "/ideas"}, new String[]{"POST", t + "/ideas"},
                new String[]{"POST", t + "/cards"}, new String[]{"POST", t + "/cards/" + card + "/convert"},
                new String[]{"PATCH", t + "/cards/" + card + "/move"}, new String[]{"DELETE", t + "/cards/" + card},
                new String[]{"GET", t + "/cards/" + card + "/history"}, new String[]{"POST", t + "/cards/" + card + "/vote"},
                new String[]{"PUT", t + "/columns/order"}, new String[]{"POST", t + "/columns"},
                new String[]{"PATCH", t + "/columns/" + team.columnIds().get(0)}, new String[]{"DELETE", t + "/columns/" + team.columnIds().get(0)},
                new String[]{"POST", t + "/cards/" + card + "/tasks"}, new String[]{"PATCH", t + "/cards/" + card + "/tasks/" + task},
                new String[]{"PUT", t + "/cards/" + card + "/tasks/order"}, new String[]{"PATCH", t + "/cards/" + card + "/tasks/" + task + "/toggle"},
                new String[]{"DELETE", t + "/cards/" + card + "/tasks/" + task},
                new String[]{"GET", t + "/tags"}, new String[]{"POST", t + "/tags"},
                new String[]{"PATCH", t + "/tags/" + team.tagIds().get(0)}, new String[]{"DELETE", t + "/tags/" + team.tagIds().get(0)},
                new String[]{"GET", "/api/v1/kanban/" + card}, new String[]{"PUT", "/api/v1/kanban/" + card}, new String[]{"DELETE", "/api/v1/kanban/" + card},
                new String[]{"GET", "/api/v1/kanban/" + card + "/script"}, new String[]{"PUT", "/api/v1/kanban/" + card + "/script"},
                new String[]{"GET", "/api/v1/kanban/" + card + "/script/export"}, new String[]{"GET", "/api/v1/kanban/" + card + "/script/versions"},
                new String[]{"POST", "/api/v1/kanban/" + card + "/script/versions"}, new String[]{"POST", "/api/v1/kanban/" + card + "/script/versions/1/restore"});
        for (String[] route : routes) {
            boolean withBody = !route[0].equals("GET") && !route[0].equals("DELETE");
            Resp r = api.send(route[0], route[1], null, withBody ? body : null);
            assertEquals(401, r.status(), route[0] + " " + route[1] + " -> " + r.body());
        }
        // and nothing was changed by the unauthenticated writes
        assertEquals(1, api.get(t + "/board", ana.token()).at("columns.0.cards").size());
        assertEquals(5, api.get(t + "/tags", ana.token()).size());
    }

    @Test
    @DisplayName("SEC-14 Cross-team matrix: a user from another team never reads or changes this team's card (404/403)")
    void crossTeamMatrix() {
        Team team = newTeam();
        long card = projectId(team, "Alheio");
        long task = taskId(team, card, "t");
        long idea = ideaId(team, "Ideia alheia");
        String k = "/api/v1/kanban/" + card;
        for (Resp r : List.of(
                api.get(k, bia.token()), api.put(k, bia.token(), map("title", "hack")), api.delete(k, bia.token()),
                api.get(k + "/script", bia.token()), api.put(k + "/script", bia.token(), map("content", "hack")),
                api.get(k + "/script/export", bia.token()), api.get(k + "/script/versions", bia.token()),
                api.post(k + "/script/versions", bia.token(), null),
                api.post(otherTeam.path() + "/cards/" + idea + "/vote", bia.token(), null),
                api.patch(otherTeam.path() + "/cards/" + card + "/move", bia.token(), map("columnId", otherTeam.columnIds().get(0))),
                api.delete(otherTeam.path() + "/cards/" + card, bia.token()),
                api.patch(otherTeam.path() + "/cards/" + card + "/tasks/" + task, bia.token(), map("title", "hack")),
                api.put(otherTeam.path() + "/cards/" + card + "/tasks/order", bia.token(), map("taskIds", List.of(task))))) {
            assertTrue(r.status() == 404, "expected 404, got " + r.status() + " " + r.body());
        }
        for (Resp r : List.of(
                api.get(team.path() + "/board", bia.token()), api.get(team.path() + "/tags", bia.token()),
                api.post(team.path() + "/tags", bia.token(), map("name", "hack", "color", "#112233")),
                api.put(team.path() + "/columns/order", bia.token(), map("columnIds", team.columnIds())),
                api.get(team.path() + "/cards/" + card + "/history", bia.token()),
                api.delete(team.path() + "/cards/" + card, bia.token()))) {
            assertEquals(403, r.status(), r.body());
        }
        Resp still = api.get(k, ana.token());
        assertEquals("Alheio", still.text("title"));
        assertEquals(1, api.get(team.path() + "/board", ana.token()).at("columns.0.cards.0.tasks").size());
    }

    @Test
    @DisplayName("SEC-04 Passwords are stored as BCrypt hashes, never in clear text")
    void passwordsHashed() {
        List<String> hashes = jdbc.queryForList("SELECT password FROM users", String.class);
        assertFalse(hashes.isEmpty());
        for (String hash : hashes) {
            assertTrue(hash.matches("^\\$2[aby]\\$\\d\\d\\$.{53}$"), "not a BCrypt hash");
            assertNotEquals(PASSWORD, hash);
        }
    }

    @Test
    @DisplayName("SEC-04b User JSON never exposes the password hash")
    void noPasswordInResponses() {
        Resp me = api.get("/api/v1/users/" + ana.id(), ana.token());
        assertEquals(200, me.status(), me.body());
        assertFalse(me.body().contains("password"));
        assertFalse(me.body().contains("$2"));
        Resp members = api.get(newTeam().path() + "/members", ana.token());
        assertFalse(members.body().contains("password"));
        int other = api.get("/api/v1/users/" + bia.id(), ana.token()).status();
        assertTrue(other == 403 || other == 404, "reading another user's profile is refused, got " + other);
    }

    @Test
    @DisplayName("SEC-06 SQL injection text in login and query parameters is treated as data")
    void sqlInjection() {
        Resp login = api.post("/api/auth/login", null, map("email", "' OR '1'='1", "password", "' OR '1'='1"));
        assertTrue(login.status() == 400 || login.status() == 401, "login with injection -> " + login.status());
        Team team = newTeam();
        ideaId(team, "normal");
        assertEquals(0, api.get(team.path() + "/ideas?q=" + "%27%3B%20DROP%20TABLE%20kanban_items%3B--", ana.token()).size());
        assertEquals(400, api.get(team.path() + "/ideas?tagId=1%20OR%201=1", ana.token()).status());
        assertEquals(1, api.get(team.path() + "/ideas", ana.token()).size());
        Resp tag = api.post(team.path() + "/tags", ana.token(), map("name", "'; DROP TABLE tags;--", "color", "#112233"));
        assertEquals(200, tag.status());
        assertEquals("'; DROP TABLE tags;--", tag.text("name"));
        assertEquals(6, api.get(team.path() + "/tags", ana.token()).size());
    }

    @Test
    @DisplayName("SEC-09 CORS: preflight from the allowed origin passes, from an unknown origin is rejected")
    void cors() {
        Resp ok = api.send("OPTIONS", "/api/v1/teams", null, null, Map.of("Origin", "http://localhost:3000",
                "Access-Control-Request-Method", "PATCH", "Access-Control-Request-Headers", "authorization,content-type"));
        assertEquals(200, ok.status());
        assertEquals("http://localhost:3000", ok.header("Access-Control-Allow-Origin"));
        assertTrue(ok.header("Access-Control-Allow-Methods").contains("PATCH"));

        Resp evil = api.send("OPTIONS", "/api/v1/teams", null, null, Map.of("Origin", "http://evil.example",
                "Access-Control-Request-Method", "GET"));
        assertEquals(403, evil.status());
        assertNull(evil.header("Access-Control-Allow-Origin"));
        Resp actual = api.send("GET", "/api/v1/teams", ana.token(), null, Map.of("Origin", "http://evil.example"));
        assertEquals(403, actual.status(), "a simple cross-origin request from an unknown origin is refused");
    }

    @Test
    @DisplayName("SEC-11 Basic security headers are present (nosniff, frame options)")
    void securityHeaders() {
        Resp r = api.get("/api/v1/teams", null);
        assertEquals("nosniff", r.header("X-Content-Type-Options"));
        assertNotNull(r.header("X-Frame-Options"));
    }

    @Test
    @DisplayName("SEC-11b Actuator exposes only health (no env/beans/heapdump) without authentication")
    void actuatorExposure() {
        assertEquals(200, api.get("/actuator/health", null).status());
        for (String endpoint : List.of("env", "beans", "heapdump", "mappings", "configprops", "loggers")) {
            int status = api.get("/actuator/" + endpoint, null).status();
            assertTrue(status == 404 || status == 401 || status == 403, "/actuator/" + endpoint + " answered " + status);
        }
    }

    @Test
    @DisplayName("SEC-10 Excessive login attempts are blocked with 429")
    void rateLimit() {
        // own user: the limit is per IP + e-mail, so the other fixtures are not affected
        Person victim = person("ratelimit");
        int max = Integer.parseInt(environment.getProperty("app.rate-limit.max-attempts"));
        Resp last = null;
        for (int i = 0; i < max + 2; i++) last = api.post("/api/auth/login", null, map("email", victim.email(), "password", "wrong"));
        assertEquals(429, last.status());
        assertEquals("Muitas tentativas. Aguarde alguns minutos e tente novamente.", last.text("message"));
        assertNotNull(last.header("Retry-After"));
        assertTrue(Integer.parseInt(last.header("Retry-After")) > 0);
        // even the right password is blocked in the window, but another e-mail is not
        assertEquals(429, api.post("/api/auth/login", null, map("email", victim.email(), "password", PASSWORD)).status());
        assertEquals(200, api.post("/api/auth/login", null, map("email", ana.email(), "password", PASSWORD)).status());
    }

    @Test
    @DisplayName("SEC-07 Dangerous HTML in a script is sanitized before it is stored")
    void xssSanitized() {
        Team team = newTeam();
        long card = projectId(team, "xss");
        api.put("/api/v1/kanban/" + card + "/script", ana.token(), map("content", "<p>ok</p><script>alert(1)</script><img src=x onerror=alert(1)>"));
        String stored = api.get("/api/v1/kanban/" + card + "/script", ana.token()).text("content");
        assertFalse(stored.contains("<script"));
        assertFalse(stored.contains("onerror"));
        assertTrue(stored.contains("<p>ok</p>"));
    }
}
