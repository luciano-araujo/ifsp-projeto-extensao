package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.entity.PlanType;
import br.edu.ifsp.tcc.application.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QA of milestone M5 (plans), second layer on top of {@link PlanIT}: exact boundaries of every limit and plan,
 * attempts to upgrade a team without the system administrator, trial and expiry at the exact second (the
 * application Clock is replaced by a controllable one), invite seats, the free-team rule and its bypass attempts,
 * the approve / change-request cycle, validation of the admin routes and the documented race conditions.
 * Plan cases: IT-PLN-01..05, UT-110..119 (integration side), SEC-24, SEC-25, DB (plan columns).
 */
@Import({MailCapture.Config.class, QaM5PlansIT.TestClockConfig.class})
class QaM5PlansIT extends AbstractIT {

    private static final long MB = 1024L * 1024;
    private static final long GB = 1024L * MB;

    /** A clock that follows the system until a test freezes it at a moment. */
    static final class ControlledClock extends Clock {
        private volatile Instant frozen;

        void freeze(Instant at) { frozen = at; }
        void release() { frozen = null; }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { Instant at = frozen; return at != null ? at : Instant.now(); }
    }

    static final ControlledClock CLOCK = new ControlledClock();

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        Clock controlledClock() {
            return CLOCK;
        }
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @AfterEach
    void releaseClock() {
        CLOCK.release();
    }

    // --- Fixtures ---

    private record Free(Person admin, Team team) {
        String path() { return team.path(); }
        String token() { return admin.token(); }
    }

    /** A user with one shared team on Gratuito (the personal team is created and ignored). */
    private Free freeTeam(String prefix) {
        Person admin = person(prefix);
        assertEquals(200, api.get("/api/v1/teams", admin.token()).status());
        Resp created = api.post("/api/v1/teams", admin.token(), map("name", "Gratuita " + UUID.randomUUID().toString().substring(0, 6)));
        assertEquals(200, created.status(), created.body());
        long id = created.num("id");
        List<Long> columns = new ArrayList<>();
        api.get("/api/v1/teams/" + id + "/board", admin.token()).at("columns").forEach(c -> columns.add(c.path("id").asLong()));
        return new Free(admin, new Team(id, 0, 0, 0, columns, List.of()));
    }

    private Resp project(Free f, String title) {
        return createProject(f.team(), f.admin(), title, f.team().columnIds().get(0));
    }

    private long projectOk(Free f, String title) {
        Resp r = project(f, title);
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    private Person sysadmin() {
        if (userRepository.findByEmail(PlanIT.SYSADMIN_EMAIL).isEmpty()) {
            User user = new User();
            user.setEmail(PlanIT.SYSADMIN_EMAIL);
            user.setName("Admin Sistema");
            user.setPassword(passwordEncoder.encode(PASSWORD));
            userRepository.save(user);
        }
        Resp login = api.post("/api/auth/login", null, map("email", PlanIT.SYSADMIN_EMAIL, "password", PASSWORD));
        assertEquals(200, login.status(), login.body());
        return new Person(0, PlanIT.SYSADMIN_EMAIL, login.text("token"));
    }

    private Resp activate(long teamId, String plan, String expiresAt) {
        Object body = expiresAt == null ? map("plan", plan) : map("plan", plan, "expiresAt", expiresAt);
        return api.put("/api/v1/admin/teams/" + teamId + "/plan", sysadmin().token(), body);
    }

    private Resp plan(Free f) {
        return api.get(f.path() + "/plan", f.token());
    }

    private String dbPlan(long teamId) {
        return jdbc.queryForObject("SELECT CONCAT(plan, '/', plan_source, '/', IFNULL(plan_expires_at, 'null'), '/', IFNULL(trial_used_at, 'null')) "
                + "FROM teams WHERE id = ?", String.class, teamId);
    }

    private Resp upload(String teamPath, String token, long cardId, int size) {
        String boundary = "----cw" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] content = new byte[size];
        java.util.Arrays.fill(content, (byte) 'a');
        byte[] magic = "%PDF-".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(magic, 0, content, 0, Math.min(magic.length, size));
        try {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(content);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:"
                            + environment.getProperty("local.server.port") + teamPath + "/cards/" + cardId + "/materials"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private void insertMaterialRow(long cardId, long sizeBytes) {
        jdbc.update("INSERT INTO materials (uuid, kanban_item_id, file_name, content_type, size_bytes, storage_key) "
                + "VALUES (?, ?, 'volume.pdf', 'application/pdf', ?, ?)", UUID.randomUUID().toString(), cardId,
                sizeBytes, UUID.randomUUID().toString());
    }

    private static String iso(Instant at) {
        return at.truncatedTo(ChronoUnit.SECONDS).toString();
    }

    // --- IT-PLN-01 / UT-110: catalogue and the plan a new team gets ---

    @Test
    @DisplayName("IT-PLN-01 / UT-110 GET /plans: every number of the four plans, and a new team starts on Gratuito with those limits")
    void catalogueNumbersAndNewTeam() {
        Resp r = api.get("/api/v1/plans", ana.token());
        assertEquals(200, r.status(), r.body());
        Object[][] expected = {
                {"GRATUITO", "Gratuito", 0.0, 1, 3, 1, false, 100 * MB},
                {"SOLO", "Solo", 49.0, 1, 10, 5, false, GB},
                {"PRO", "Pro", 99.0, 1, null, null, true, 5 * GB},
                {"AGENCIA", "Agência", 249.0, 5, null, null, true, 20 * GB},
        };
        for (int i = 0; i < 4; i++) {
            String p = i + ".";
            assertEquals(expected[i][0], r.text(p + "plan"));
            assertEquals(expected[i][1], r.text(p + "name"));
            assertEquals((double) expected[i][2], r.at(p + "monthlyPrice").asDouble(), 0.001);
            assertEquals(expected[i][3], r.at(p + "limits.maxUsers").asInt());
            if (expected[i][4] == null) assertTrue(r.at(p + "limits.maxActiveProjects").isNull());
            else assertEquals(expected[i][4], r.at(p + "limits.maxActiveProjects").asInt());
            if (expected[i][5] == null) assertTrue(r.at(p + "limits.maxCourses").isNull());
            else assertEquals(expected[i][5], r.at(p + "limits.maxCourses").asInt());
            assertEquals(expected[i][6], r.at(p + "limits.automaticSummary").asBoolean());
            assertEquals((long) expected[i][7], r.at(p + "limits.storageBytes").asLong());
        }

        Free f = freeTeam("novo");
        Resp teamPlan = plan(f);
        assertEquals("GRATUITO", teamPlan.text("plan"));
        assertEquals("FREE", teamPlan.text("source"));
        assertTrue(teamPlan.at("expiresAt").isNull());
        assertTrue(teamPlan.at("trialAvailable").asBoolean());
        assertTrue(teamPlan.at("trialUsedAt").isNull());
        assertEquals(1, teamPlan.at("limits.maxUsers").asInt());
        assertEquals(3, teamPlan.at("limits.maxActiveProjects").asInt());
        assertEquals(1, teamPlan.at("limits.maxCourses").asInt());
        assertFalse(teamPlan.at("limits.automaticSummary").asBoolean());
        assertEquals(100 * MB, teamPlan.at("limits.storageBytes").asLong());
        assertEquals(0, teamPlan.at("usage.activeProjects").asInt());
        assertEquals(0, teamPlan.at("usage.storageBytes").asLong());
        assertFalse(teamPlan.at("overLimit").asBoolean());
    }

    @Test
    @DisplayName("IT-PLN-01 GET team plan by role: every member reads it, outsiders and anonymous do not; only the ADMIN starts a trial")
    void teamPlanByRole() {
        Team team = newTeam();
        for (Person p : List.of(ana, carlos, rui)) {
            assertEquals(200, api.get(team.path() + "/plan", p.token()).status());
        }
        assertEquals(403, api.get(team.path() + "/plan", bia.token()).status());
        assertEquals(403, api.get(team.path() + "/plan", loner.token()).status());
        assertEquals(401, api.get(team.path() + "/plan", null).status());
        assertEquals(403, api.get("/api/v1/teams/999999999/plan", ana.token()).status(), "unknown team: same answer as another team's");
        for (Person p : List.of(carlos, rui, bia, loner)) {
            assertEquals(403, api.post(team.path() + "/plan/trial", p.token(), map("plan", "PRO")).status());
        }
        assertEquals(401, api.post(team.path() + "/plan/trial", null, map("plan", "PRO")).status());
    }

    // --- SEC-24 / SEC-25: nobody upgrades a team without the system administrator ---

    @Test
    @DisplayName("SEC-24/25 A team ADMIN cannot upgrade the plan by any route: admin routes, mass assignment, invented routes")
    void selfUpgradeAttempts() {
        Free f = freeTeam("hack");
        String before = dbPlan(f.team().id());

        assertEquals(403, api.put("/api/v1/admin/teams/" + f.team().id() + "/plan", f.token(), map("plan", "AGENCIA")).status());
        assertEquals(403, api.get("/api/v1/admin/teams?email=" + f.admin().email(), f.token()).status());
        assertEquals(403, api.put("/api/v1/admin/teams/" + f.team().id() + "/plan", f.token(),
                map("plan", "AGENCIA", "expiresAt", "2037-01-01T00:00:00Z")).status());

        // Fields the client has no business sending: ignored (200) or refused (400), never applied
        Resp rename = api.patch(f.path(), f.token(), map("name", "Renomeada", "plan", "AGENCIA", "planSource", "MANUAL",
                "planExpiresAt", "2037-01-01T00:00:00Z", "trialUsedAt", null, "personal", true));
        assertTrue(rename.status() == 200 || rename.status() == 400, rename.body());
        Resp create = api.post("/api/v1/teams", person("hack2").token(), map("name", "Nova", "plan", "AGENCIA", "planSource", "MANUAL"));
        assertTrue(create.status() == 200 || create.status() == 400, create.body());
        if (create.status() == 200) {
            assertEquals("GRATUITO", create.text("plan"));
            assertEquals("FREE", create.text("planSource"));
        }
        Resp member = api.post(f.path() + "/members", f.token(), map("email", carlos.email(), "role", "CREATOR", "plan", "AGENCIA"));
        assertEquals(402, member.status(), member.body());

        // Routes that do not exist must not be a back door
        for (String method : List.of("PUT", "POST", "PATCH")) {
            Resp r = api.send(method, f.path() + "/plan", f.token(), map("plan", "AGENCIA"));
            assertTrue(r.status() == 404 || r.status() == 405, method + " " + r.status() + " " + r.body());
        }
        assertEquals(404, api.send("PUT", "/api/v1/teams/" + f.team().id() + "/plan/activate", f.token(), map("plan", "PRO")).status());

        // The trial cannot be asked for the free plan, a lower-case plan or nothing
        assertEquals(400, api.post(f.path() + "/plan/trial", f.token(), map("plan", "GRATUITO")).status());
        assertEquals(400, api.post(f.path() + "/plan/trial", f.token(), map("plan", "pro")).status());
        assertEquals(400, api.post(f.path() + "/plan/trial", f.token(), "{}").status());
        assertEquals(400, api.post(f.path() + "/plan/trial", f.token(), "not json").status());

        String after = dbPlan(f.team().id());
        assertTrue(after.startsWith("GRATUITO/FREE/null/null"), "unchanged except the name: " + before + " -> " + after);
        assertFalse(jdbc.queryForObject("SELECT personal FROM teams WHERE id = ?", Boolean.class, f.team().id()));
    }

    @Test
    @DisplayName("SEC-24 Admin routes: 401 anonymous, 403 for every team role and outsider; the system administrator passes")
    void adminRoutesAccessMatrix() {
        Team team = newTeam();
        String put = "/api/v1/admin/teams/" + team.id() + "/plan";
        String get = "/api/v1/admin/teams?email=" + ana.email();
        assertEquals(401, api.put(put, null, map("plan", "PRO")).status());
        assertEquals(401, api.get(get, null).status());
        for (Person p : List.of(ana, carlos, rui, bia, loner)) {
            Resp w = api.put(put, p.token(), map("plan", "PRO"));
            assertEquals(403, w.status(), w.body());
            assertEquals("Apenas o administrador do sistema pode executar esta ação.", w.text("message"));
            assertEquals(403, api.get(get, p.token()).status());
        }
        assertEquals("AGENCIA", dbPlan(team.id()).split("/")[0], "403s changed nothing");
        Person sys = sysadmin();
        assertEquals(200, api.get(get, sys.token()).status());
        assertEquals(200, api.put(put, sys.token(), map("plan", "AGENCIA")).status());
    }

    @Test
    @DisplayName("IT-PLN-04 Admin PUT validation: date format, 2037/2038 edge, past, unknown plan, GRATUITO ignores the date; GET needs an e-mail")
    void adminRouteValidation() {
        Free f = freeTeam("adm");
        String put = "/api/v1/admin/teams/" + f.team().id() + "/plan";
        Person sys = sysadmin();

        assertEquals(400, api.put(put, sys.token(), "{\"plan\":\"pro\"}").status());
        assertEquals(400, api.put(put, sys.token(), "{\"plan\":null}").status());
        assertEquals(400, api.put(put, sys.token(), "{\"plan\":\"PRO\",\"expiresAt\":\"2030-01-01T00:00:00\"}").status(), "no Z");
        assertEquals(400, api.put(put, sys.token(), "{\"plan\":\"PRO\",\"expiresAt\":\"2030-01-01T00:00:00.000Z\"}").status(), "millis");
        assertEquals(400, api.put(put, sys.token(), "{\"plan\":\"PRO\",\"expiresAt\":\"01/01/2030\"}").status());
        assertEquals(400, api.put(put, sys.token(), map("plan", "PRO", "expiresAt", iso(Instant.now().minusSeconds(1)))).status(), "past");
        assertEquals(400, api.put(put, sys.token(), map("plan", "PRO", "expiresAt", "2038-01-01T00:00:00Z")).status(), "2038");
        assertEquals(400, api.put(put, sys.token(), map("plan", "PRO", "expiresAt", "2037-12-31T23:59:60Z")).status());
        assertEquals(400, api.put("/api/v1/admin/teams/abc/plan", sys.token(), map("plan", "PRO")).status());
        assertEquals(404, api.put("/api/v1/admin/teams/999999999/plan", sys.token(), map("plan", "PRO")).status());
        assertTrue(dbPlan(f.team().id()).startsWith("GRATUITO/FREE/null"), "every refusal left the team untouched");

        Resp last = api.put(put, sys.token(), map("plan", "PRO", "expiresAt", "2037-12-31T23:59:59Z"));
        assertEquals(200, last.status(), last.body());
        assertEquals("2037-12-31T23:59:59Z", last.text("expiresAt"));
        assertEquals(200, api.put(put, sys.token(), map("plan", "PRO", "expiresAt", iso(Instant.now().plusSeconds(5)))).status());
        // GRATUITO ignores the date, even a past one
        Resp reset = api.put(put, sys.token(), map("plan", "GRATUITO", "expiresAt", "2001-01-01T00:00:00Z"));
        assertEquals(200, reset.status(), reset.body());
        assertEquals("FREE", reset.text("source"));
        assertTrue(reset.at("expiresAt").isNull());

        assertEquals(400, api.get("/api/v1/admin/teams", sys.token()).status());
        assertEquals(0, api.get("/api/v1/admin/teams?email=", sys.token()).size(), "blank e-mail finds nobody");
        assertEquals(0, api.get("/api/v1/admin/teams?email=nao.existe@creatorworkbench.test", sys.token()).size());
        Resp upper = api.get("/api/v1/admin/teams?email=" + f.admin().email().toUpperCase(), sys.token());
        assertEquals(200, upper.status());
        assertEquals(2, upper.size(), "case-insensitive lookup: personal + shared team: " + upper.body());
        // Only the teams the person ADMINISTERS are listed
        Team team = newTeam();
        Resp ofCarlos = api.get("/api/v1/admin/teams?email=" + carlos.email(), sys.token());
        assertTrue(ofCarlos.json().findValuesAsString("id").stream().noneMatch(id -> id.equals(String.valueOf(team.id()))), ofCarlos.body());
    }

    @Test
    @DisplayName("IT-PLN-04 Manual activation over a trial keeps the trial as used; GRATUITO afterwards does not give the trial back")
    void manualActivationOverTrial() {
        Free f = freeTeam("ovr");
        assertEquals(200, api.post(f.path() + "/plan/trial", f.token(), map("plan", "SOLO")).status());
        Resp manual = activate(f.team().id(), "PRO", null);
        assertEquals(200, manual.status(), manual.body());
        assertEquals("MANUAL", manual.text("source"));
        assertFalse(manual.at("trialAvailable").asBoolean());
        assertFalse(manual.at("trialUsedAt").isNull());
        assertEquals(200, activate(f.team().id(), "GRATUITO", null).status());
        Resp again = api.post(f.path() + "/plan/trial", f.token(), map("plan", "AGENCIA"));
        assertEquals(409, again.status(), again.body());
        assertEquals("O teste grátis já foi usado por esta equipe.", again.text("message"));
        assertFalse(plan(f).at("trialAvailable").asBoolean());
    }

    // --- IT-PLN-03 / UT-116: trial at the exact second ---

    @Test
    @DisplayName("IT-PLN-03 / UT-116 Trial: 14 days to the second; one second before it is still the trial, at the second it is Gratuito everywhere")
    void trialAtTheExactBoundary() {
        Instant t0 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        CLOCK.freeze(t0);
        Free f = freeTeam("trial");
        String path = f.path() + "/plan/trial";
        Instant expiry = t0.plus(14, ChronoUnit.DAYS);

        Resp started = api.post(path, f.token(), map("plan", "SOLO"));
        assertEquals(200, started.status(), started.body());
        assertEquals(iso(expiry), started.text("expiresAt"));
        assertEquals(iso(t0), started.text("trialUsedAt"));
        assertEquals("TRIAL", started.text("source"));
        assertEquals(10, started.at("limits.maxActiveProjects").asInt());

        // Same second: a second trial is refused, whatever the plan asked for
        for (String p : List.of("SOLO", "PRO", "AGENCIA")) {
            Resp again = api.post(path, f.token(), map("plan", p));
            assertEquals(409, again.status(), again.body());
            assertEquals("O teste grátis já foi usado por esta equipe.", again.text("message"));
        }

        CLOCK.freeze(expiry.minusSeconds(1));
        Resp last = plan(f);
        assertEquals("SOLO", last.text("plan"));
        assertEquals("TRIAL", last.text("source"));
        assertEquals("SOLO", api.get("/api/v1/teams", f.token()).text("1.plan"));
        for (int i = 1; i <= 4; i++) projectOk(f, "Ainda no teste " + i);
        assertEquals(409, api.post(path, f.token(), map("plan", "PRO")).status());

        CLOCK.freeze(expiry);
        Resp gone = plan(f);
        assertEquals("GRATUITO", gone.text("plan"));
        assertEquals("FREE", gone.text("source"));
        assertTrue(gone.at("expiresAt").isNull());
        assertFalse(gone.at("trialAvailable").asBoolean());
        assertTrue(gone.at("overLimit").asBoolean(), "4 active projects on a 3-project plan");
        Resp teams = api.get("/api/v1/teams", f.token());
        assertEquals("GRATUITO", teams.text("1.plan"));
        assertEquals("FREE", teams.text("1.planSource"));
        assertTrue(teams.at("1.planExpiresAt").isNull());
        assertEquals(402, project(f, "Depois do teste").status());
        Resp admin = api.get("/api/v1/admin/teams?email=" + f.admin().email(), sysadmin().token());
        assertEquals("GRATUITO", admin.text("1.plan"));

        // Second trial after expiry, days later, with the other plans, and after the administrator reset it
        CLOCK.freeze(expiry.plus(400, ChronoUnit.DAYS));
        for (String p : List.of("SOLO", "PRO", "AGENCIA")) {
            assertEquals(409, api.post(path, f.token(), map("plan", p)).status());
        }
        assertEquals(200, activate(f.team().id(), "GRATUITO", null).status());
        assertEquals(409, api.post(path, f.token(), map("plan", "PRO")).status());
        // The row is untouched by reads: the downgrade is computed, not stored
        assertTrue(dbPlan(f.team().id()).startsWith("GRATUITO/FREE/null/"), dbPlan(f.team().id()));
    }

    @Test
    @DisplayName("IT-PLN-03 Trial refused while a paid plan is in force (409); possible again once a manual plan has expired")
    void trialAndPaidPlan() {
        Instant t0 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        CLOCK.freeze(t0);
        Free f = freeTeam("paid");
        Instant end = t0.plus(1, ChronoUnit.HOURS);
        assertEquals(200, activate(f.team().id(), "PRO", iso(end)).status());

        Resp refused = api.post(f.path() + "/plan/trial", f.token(), map("plan", "AGENCIA"));
        assertEquals(409, refused.status(), refused.body());
        assertEquals("A equipe já possui um plano pago ativo.", refused.text("message"));
        assertEquals("PRO", plan(f).text("plan"));

        CLOCK.freeze(end.minusSeconds(1));
        assertEquals(409, api.post(f.path() + "/plan/trial", f.token(), map("plan", "AGENCIA")).status());
        assertEquals("PRO", plan(f).text("plan"));

        CLOCK.freeze(end);
        assertEquals("GRATUITO", plan(f).text("plan"));
        Resp trial = api.post(f.path() + "/plan/trial", f.token(), map("plan", "AGENCIA"));
        assertEquals(200, trial.status(), "never used the trial: " + trial.body());
        assertEquals("TRIAL", trial.text("source"));
    }

    // --- UT-111..115: limits at the exact boundary ---

    @Test
    @DisplayName("UT-113 Members: Solo and Pro allow 1 (2nd is 402); Agência allows exactly 5, the 6th is 402 and nothing is stored")
    void memberLimitsPerPlan() {
        Free f = freeTeam("seats");
        Person extra = person("extra");
        for (String p : List.of("GRATUITO", "SOLO", "PRO")) {
            setPlan(f.team().id(), p);
            Resp r = api.post(f.path() + "/members", f.token(), map("email", extra.email(), "role", "CREATOR"));
            assertEquals(402, r.status(), p + " " + r.body());
            assertTrue(r.text("message").contains("plano " + PlanType.valueOf(p).getDisplayName()), r.body());
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM team_members WHERE team_id = ?", Integer.class, f.team().id()));

        setPlan(f.team().id(), "AGENCIA");
        for (int i = 2; i <= 4; i++) {
            addMemberDirect(f.team().id(), person("seat" + i), "CREATOR");
        }
        // The 5th seat goes through the route: it is an invite now (the plan counts members + open invites)
        Person fifth = person("seat5");
        Resp invited = api.post(f.path() + "/members", f.token(), map("email", fifth.email(), "role", "CREATOR"));
        assertEquals(200, invited.status(), invited.body());
        assertEquals("PENDING", invited.text("status"));
        assertEquals(4, plan(f).at("usage.members").asInt(), "an invite is not a member until accepted");
        assertEquals(200, api.post("/api/v1/invites/" + MailCapture.inviteToken(fifth.email()) + "/accept", fifth.token(), null).status());
        Resp sixth = api.post(f.path() + "/members", f.token(), map("email", extra.email(), "role", "REVIEWER"));
        assertEquals(402, sixth.status(), sixth.body());
        assertTrue(sixth.text("message").contains("5 usuários"), sixth.body());
        assertFalse(sixth.text("message").contains("mude para"), "no plan above Agência: " + sixth.body());
        assertEquals(5, plan(f).at("usage.members").asInt());
        // An unknown e-mail is not a 404 any more (decision 22: no enumeration): the full team answers 402 like for a known one
        assertEquals(402, api.post(f.path() + "/members", f.token(), map("email", "ninguem." + UUID.randomUUID() + "@creatorworkbench.test", "role", "CREATOR")).status());
    }

    @Test
    @DisplayName("UT-111 Active projects: Solo 10th ok / 11th 402, Gratuito conversion exactly at the 3rd, Pro and Agência unlimited, ideas never count")
    void activeProjectBoundaries() {
        Free f = freeTeam("proj");
        // Ideas are free: ten of them on Gratuito
        List<Long> ideas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Resp idea = api.post(f.path() + "/ideas", f.token(), map("title", "Ideia " + i, "description", "d"));
            assertEquals(200, idea.status(), idea.body());
            ideas.add(idea.num("id"));
        }
        assertEquals(0, plan(f).at("usage.activeProjects").asInt());
        long col = f.team().columnIds().get(0);
        java.util.function.Function<Long, Resp> convert = id -> api.post(f.path() + "/cards/" + id + "/convert", f.token(),
                map("title", "Convertida " + id, "targetAudience", "Estudantes", "pedagogicalObjective", "Ensinar algo importante", "columnId", col));
        projectOk(f, "Direto 1");
        projectOk(f, "Direto 2");
        assertEquals(200, convert.apply(ideas.get(0)).status(), "3rd active project by conversion is the last one allowed");
        Resp fourth = convert.apply(ideas.get(1));
        assertEquals(402, fourth.status(), fourth.body());
        assertEquals("IDEATION", idea(f, ideas.get(1)), "a refused conversion leaves the idea an idea");
        assertEquals(402, project(f, "Direto 4").status());
        assertEquals(3, plan(f).at("usage.activeProjects").asInt());

        setPlan(f.team().id(), "SOLO");
        for (int i = 4; i <= 10; i++) projectOk(f, "Solo " + i);
        assertEquals(10, plan(f).at("usage.activeProjects").asInt());
        Resp eleventh = project(f, "Solo 11");
        assertEquals(402, eleventh.status(), eleventh.body());
        assertTrue(eleventh.text("message").contains("até 10 projetos ativos"), eleventh.body());
        assertTrue(eleventh.text("message").contains("plano Pro"), eleventh.body());
        assertEquals(402, convert.apply(ideas.get(1)).status());

        setPlan(f.team().id(), "PRO");
        for (int i = 11; i <= 14; i++) projectOk(f, "Pro " + i);
        assertEquals(200, convert.apply(ideas.get(1)).status());
        setPlan(f.team().id(), "AGENCIA");
        assertEquals(200, project(f, "Agência 16").status());
    }

    private String idea(Free f, long cardId) {
        Resp ideas = api.get(f.path() + "/ideas", f.token());
        for (var n : ideas.json()) if (n.path("id").asLong() == cardId) return "IDEATION";
        return "PROJECT";
    }

    @Test
    @DisplayName("UT-112 Courses: Solo 5th ok / 6th 402, Pro unlimited, Gratuito 2nd 402 names Solo; a refused course is not stored")
    void courseBoundaries() {
        Free f = freeTeam("curs");
        assertEquals(200, api.post(f.path() + "/courses", f.token(), map("title", "Curso 1")).status());
        Resp second = api.post(f.path() + "/courses", f.token(), map("title", "Curso 2"));
        assertEquals(402, second.status(), second.body());
        assertTrue(second.text("message").contains("plano Solo"), second.body());
        setPlan(f.team().id(), "SOLO");
        for (int i = 2; i <= 5; i++) assertEquals(200, api.post(f.path() + "/courses", f.token(), map("title", "Curso " + i)).status());
        Resp sixth = api.post(f.path() + "/courses", f.token(), map("title", "Curso 6"));
        assertEquals(402, sixth.status(), sixth.body());
        assertTrue(sixth.text("message").contains("até 5 cursos"), sixth.body());
        assertTrue(sixth.text("message").contains("plano Pro"), sixth.body());
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM courses WHERE team_id = ?", Integer.class, f.team().id()));
        setPlan(f.team().id(), "PRO");
        for (int i = 6; i <= 8; i++) assertEquals(200, api.post(f.path() + "/courses", f.token(), map("title", "Curso " + i)).status());
        // Refused with 402 before 400? A blank title on a full team: validation first
        setPlan(f.team().id(), "GRATUITO");
        Resp blank = api.post(f.path() + "/courses", f.token(), map("title", ""));
        assertTrue(blank.status() == 400 || blank.status() == 402, blank.body());
    }

    @Test
    @DisplayName("UT-115 Summary: Gratuito and Solo 402, Pro and Agência pass the plan gate; a reviewer or outsider is refused by role first")
    void summaryGate() {
        Free f = freeTeam("sumo");
        long card = projectOk(f, "Resumo");
        String path = f.path() + "/cards/" + card + "/summary";
        for (String p : List.of("GRATUITO", "SOLO")) {
            setPlan(f.team().id(), p);
            Resp r = api.post(path, f.token(), null);
            assertEquals(402, r.status(), p + " " + r.body());
            assertTrue(r.text("message").contains("plano Pro"), r.body());
        }
        for (String p : List.of("PRO", "AGENCIA")) {
            setPlan(f.team().id(), p);
            assertEquals(400, api.post(path, f.token(), null).status(), p + ": past the gate, empty script is 400");
        }
        assertEquals(403, api.post(path, ana.token(), null).status());
        // Idea cards: refused as ideas (400), not billed
        long idea = api.post(f.path() + "/ideas", f.token(), map("title", "So ideia", "description", "d")).num("id");
        setPlan(f.team().id(), "GRATUITO");
        Resp onIdea = api.post(f.path() + "/cards/" + idea + "/summary", f.token(), null);
        assertTrue(onIdea.status() == 400 || onIdea.status() == 402, onIdea.body());
    }

    @Test
    @DisplayName("UT-114 / SEC-25 Storage: for every plan the file that reaches exactly the limit is accepted and one byte over is 402")
    void storageExactBytesEveryPlan() {
        for (PlanType plan : PlanType.values()) {
            Free f;
            if (plan == PlanType.AGENCIA) {
                Person admin = person("stor");
                Team t = newTeamFor(admin, "Armazenamento " + UUID.randomUUID().toString().substring(0, 5), null, null);
                f = new Free(admin, t);
            } else {
                f = freeTeam("stor");
                setPlan(f.team().id(), plan.name());
            }
            long limit = plan.getStorageBytes();
            long card = projectOk(f, "Material " + plan);
            insertMaterialRow(card, limit - 5);

            // 4 bytes of room can not hold a 5-byte file: one byte over -> 402, nothing stored
            jdbc.update("UPDATE materials SET size_bytes = ? WHERE kanban_item_id = ?", limit - 4, card);
            Resp oneOver = upload(f.path(), f.token(), card, 5);
            assertEquals(402, oneOver.status(), plan + " one byte over: " + oneOver.body());
            assertEquals(limit - 4, plan(f).at("usage.storageBytes").asLong(), "the refused file is not stored");
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM materials WHERE kanban_item_id = ?", Integer.class, card));

            // 5 bytes of room: lands exactly on the limit
            jdbc.update("UPDATE materials SET size_bytes = ? WHERE kanban_item_id = ?", limit - 5, card);
            Resp exact = upload(f.path(), f.token(), card, 5);
            assertEquals(200, exact.status(), plan + " exactly at the limit: " + exact.body());
            Resp usage = plan(f);
            assertEquals(limit, usage.at("usage.storageBytes").asLong());
            assertFalse(usage.at("overLimit").asBoolean(), "exactly at the limit is not over it");
            Resp next = upload(f.path(), f.token(), card, 5);
            assertEquals(402, next.status(), plan + " " + next.body());
            if (plan == PlanType.AGENCIA) assertFalse(next.text("message").contains("mude para"), next.body());
            else assertTrue(next.text("message").contains("mude para o plano"), next.body());

            // Deleting the material gives the space back
            long id = api.get(f.path() + "/cards/" + card + "/materials", f.token()).json().get(0).path("id").asLong();
            for (var m : api.get(f.path() + "/cards/" + card + "/materials", f.token()).json()) {
                if (m.path("sizeBytes").asLong() == 5) id = m.path("id").asLong();
            }
            assertEquals(204, api.delete(f.path() + "/materials/" + id, f.token()).status());
            assertEquals(limit - 5, plan(f).at("usage.storageBytes").asLong());
            assertEquals(200, upload(f.path(), f.token(), card, 5).status(), plan + " space freed by the delete");
        }
    }

    // --- UT-111: approve / un-approve ---

    @Test
    @DisplayName("UT-111 Approve frees a slot, a later CHANGES_REQUESTED takes it back (no deletion, only creation blocked); the latest review wins even within one second")
    void approveThenChangesRequestedCycle() {
        Free f = freeTeam("cycle");
        long p1 = projectOk(f, "Proj 1");
        long p2 = projectOk(f, "Proj 2");
        long p3 = projectOk(f, "Proj 3");
        String reviews = f.path() + "/cards/" + p1 + "/reviews";

        assertEquals(402, project(f, "Proj 4").status());
        assertEquals(200, api.post(reviews, f.token(), map("decision", "APPROVED")).status());
        assertEquals(2, plan(f).at("usage.activeProjects").asInt());
        long p4 = projectOk(f, "Proj 4");
        assertEquals(3, plan(f).at("usage.activeProjects").asInt());
        assertEquals(402, project(f, "Proj 5").status());

        // Un-approve: the 4th project already exists, the team is over the limit and only creation is blocked
        assertEquals(200, api.post(reviews, f.token(), map("decision", "CHANGES_REQUESTED", "comment", "Ajustar")).status());
        Resp over = plan(f);
        assertEquals(4, over.at("usage.activeProjects").asInt());
        assertTrue(over.at("overLimit").asBoolean());
        assertEquals(402, project(f, "Proj 5").status());
        assertEquals(200, api.get(f.path() + "/board", f.token()).status());
        assertEquals(200, api.put("/api/v1/kanban/" + p1, f.token(), map("title", "Proj 1 editado")).status());
        assertEquals(200, api.post(f.path() + "/cards/" + p2 + "/tasks", f.token(), map("title", "Tarefa")).status());

        // Deleting one project is not enough (3 active is the limit); deleting a second frees a slot
        assertEquals(204, api.delete(f.path() + "/cards/" + p4, f.token()).status());
        assertEquals(402, project(f, "Proj 5").status(), "exactly at the limit (3 active) still blocks creation");
        assertEquals(200, api.post(f.path() + "/cards/" + p3 + "/reviews", f.token(), map("decision", "APPROVED")).status());
        assertEquals(200, project(f, "Proj 5").status());

        // Rapid fire inside the same second: the last decision always decides
        for (int i = 0; i < 6; i++) {
            String decision = i % 2 == 0 ? "APPROVED" : "CHANGES_REQUESTED";
            assertEquals(200, api.post(reviews, f.token(), map("decision", decision, "comment", "rodada " + i)).status());
            int active = plan(f).at("usage.activeProjects").asInt();
            // p1 + p2 + p5 are active, p3 is approved; p1 counts only while its latest review is not APPROVED
            assertEquals("APPROVED".equals(decision) ? 2 : 3, active, "after " + decision + " (round " + i + ")");
        }
    }

    // --- UT-113: invite seats ---

    @Test
    @DisplayName("UT-113 Invite seats: members + open invites; expired and cancelled invites free the seat, accepted ones are not counted twice; direct add can eat reserved seats")
    void inviteSeatCounting() {
        Person admin = person("seatadm");
        Team team = newTeamFor(admin, "Assentos " + UUID.randomUUID().toString().substring(0, 5), null, null);
        String invites = team.path() + "/invites";
        Person p1 = person("inva");
        Person p2 = person("invb");
        Person p3 = person("invc");
        String e4 = "inv4." + UUID.randomUUID() + "@creatorworkbench.test";
        String e5 = "inv5." + UUID.randomUUID() + "@creatorworkbench.test";
        String e6 = "inv6." + UUID.randomUUID() + "@creatorworkbench.test";

        assertEquals(200, api.post(invites, admin.token(), map("email", p1.email(), "role", "CREATOR")).status());
        Resp i2 = api.post(invites, admin.token(), map("email", p2.email(), "role", "CREATOR"));
        assertEquals(200, i2.status(), i2.body());
        assertEquals(200, api.post(invites, admin.token(), map("email", p3.email(), "role", "REVIEWER")).status());
        assertEquals(200, api.post(invites, admin.token(), map("email", e4, "role", "CREATOR")).status());
        // 1 member + 4 open invites = the 5 seats of Agência
        Resp full = api.post(invites, admin.token(), map("email", e5, "role", "CREATOR"));
        assertEquals(402, full.status(), full.body());
        assertTrue(full.text("message").contains("convites pendentes"), full.body());

        // An expired invite frees its seat
        String tokenP1 = MailCapture.inviteToken(p1.email());
        jdbc.update("UPDATE team_invites SET expires_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 SECOND) WHERE team_id = ? AND email = ?",
                team.id(), p1.email());
        assertEquals(404, api.post("/api/v1/invites/" + tokenP1 + "/accept", p1.token(), null).status(), "expired invite cannot be accepted");
        assertEquals(200, api.post(invites, admin.token(), map("email", e5, "role", "CREATOR")).status(), "seat of the expired invite is free");
        assertEquals(402, api.post(invites, admin.token(), map("email", e6, "role", "CREATOR")).status());
        // The expired invite of p1 is replaced when p1 is invited again, which needs a free seat
        assertEquals(402, api.post(invites, admin.token(), map("email", p1.email(), "role", "CREATOR")).status());

        // A cancelled invite frees its seat
        assertEquals(204, api.delete(invites + "/" + i2.num("id"), admin.token()).status());
        assertEquals(200, api.post(invites, admin.token(), map("email", p1.email(), "role", "CREATOR")).status());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM team_invites WHERE team_id = ? AND email = ?", Integer.class, team.id(), p1.email()),
                "the expired invite was replaced, not duplicated");

        // Accepting turns an open invite into a member: the seat count does not change
        String tokenP3 = MailCapture.inviteToken(p3.email());
        assertEquals(200, api.post("/api/v1/invites/" + tokenP3 + "/accept", p3.token(), null).status());
        assertEquals(402, api.post(invites, admin.token(), map("email", e6, "role", "CREATOR")).status(), "2 members + 3 open = 5 seats");

        // Seats taken straight in the database (fixture): more members than the open invites left room for
        for (int i = 0; i < 3; i++) {
            addMemberDirect(team.id(), person("direct" + i), "CREATOR");
        }
        assertEquals(5, plan(new Free(admin, team)).at("usage.members").asInt());
        assertEquals(402, api.post(team.path() + "/members", admin.token(), map("email", person("late").email(), "role", "CREATOR")).status());
        String tokenP1b = MailCapture.inviteToken(p1.email());
        Resp late = api.post("/api/v1/invites/" + tokenP1b + "/accept", p1.token(), null);
        assertEquals(402, late.status(), "the team is full when the invite is accepted: " + late.body());
    }

    // --- UT-118: over the limit ---

    @Test
    @DisplayName("UT-118 Over the limit nothing is deleted and only creation is blocked; the team unblocks when usage is back under the limit")
    void overLimitOnlyBlocksCreation() {
        Team team = newTeam();                       // Agência with ana, carlos and rui
        Free f = new Free(ana, team);
        long card = projectOk(f, "Existente");
        setPlan(team.id(), "GRATUITO");
        Resp over = plan(f);
        assertTrue(over.at("overLimit").asBoolean());
        assertEquals(3, over.at("usage.members").asInt());
        assertEquals(402, api.post(team.path() + "/members", ana.token(), map("email", bia.email(), "role", "CREATOR")).status());
        assertEquals(402, api.post(team.path() + "/invites", ana.token(), map("email", "x." + UUID.randomUUID() + "@creatorworkbench.test", "role", "CREATOR")).status());
        assertEquals(3, api.get(team.path() + "/members", ana.token()).size(), "nobody was removed");
        assertEquals(200, api.get(team.path() + "/board", carlos.token()).status());
        assertEquals(200, api.post(team.path() + "/cards/" + card + "/tasks", carlos.token(), map("title", "Ainda edita")).status());
        assertEquals(200, project(f, "Outro projeto").status(), "projects are within their own limit");

        // Back to one member: the team is no longer over, and still full (1 user)
        for (var m : api.get(team.path() + "/members", ana.token()).json()) {
            if (m.path("userId").asLong() != ana.id()) assertEquals(204, api.delete(team.path() + "/members/" + m.path("id").asLong(), ana.token()).status());
        }
        assertFalse(plan(f).at("overLimit").asBoolean());
        assertEquals(402, api.post(team.path() + "/members", ana.token(), map("email", bia.email(), "role", "CREATOR")).status());
        setPlan(team.id(), "AGENCIA");
    }

    // --- UT-119 / SEC-25: the one-free-team rule ---

    @Test
    @DisplayName("UT-119 / SEC-25 Free team bypass attempts: the personal flag decides, never the name; renaming changes nothing; first POST /teams before any GET /teams")
    void freeTeamRuleAndNames() {
        // A shared team called like the personal one still counts as the free team
        Person v = person("vera");
        assertEquals("Estúdio de Vera", api.get("/api/v1/teams", v.token()).text("0.name"));
        Resp lookalike = api.post("/api/v1/teams", v.token(), map("name", "Estúdio de Vera"));
        assertEquals(200, lookalike.status(), lookalike.body());
        assertFalse(lookalike.at("personal").asBoolean());
        assertEquals(402, api.post("/api/v1/teams", v.token(), map("name", "Estúdio de Vera 2")).status());
        assertEquals(402, api.post("/api/v1/teams", v.token(), map("name", "Outra")).status());
        // Renaming the shared team to a personal-looking name or to anything else changes nothing
        assertEquals(200, api.patch("/api/v1/teams/" + lookalike.num("id"), v.token(), map("name", "Meu Estúdio")).status());
        assertEquals(402, api.post("/api/v1/teams", v.token(), map("name", "Terceira")).status());

        // Renaming the personal team does not make it a counted free team and does not free the shared one
        Person w = person("wanda");
        long personalId = api.get("/api/v1/teams", w.token()).num("0.id");
        Resp renamed = api.patch("/api/v1/teams/" + personalId, w.token(), map("name", "Equipe Qualquer"));
        assertEquals(200, renamed.status(), renamed.body());
        assertTrue(renamed.at("personal").asBoolean());
        assertEquals(200, api.post("/api/v1/teams", w.token(), map("name", "Compartilhada")).status());
        assertEquals(402, api.post("/api/v1/teams", w.token(), map("name", "Compartilhada 2")).status());
        assertEquals(409, api.delete("/api/v1/teams/" + personalId, w.token()).status(), "personal team survives renaming");

        // A brand-new user whose first call is POST /teams (no personal team yet): one free team, then 402
        Person u = person("ursula");
        assertEquals(200, api.post("/api/v1/teams", u.token(), map("name", "Primeira")).status());
        assertEquals(402, api.post("/api/v1/teams", u.token(), map("name", "Segunda")).status());
        Resp teams = api.get("/api/v1/teams", u.token());
        assertEquals(1, teams.size(), "no personal team is created once the user has a team: " + teams.body());
    }

    @Test
    @DisplayName("UT-119 A trial still counts as the free team (no chaining); an active manual plan does not, an expired one counts again")
    void freeTeamRuleWithTrialAndManualPlans() {
        Instant t0 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        CLOCK.freeze(t0);
        Free trial = freeTeam("tchain");
        assertEquals(200, api.post(trial.path() + "/plan/trial", trial.token(), map("plan", "AGENCIA")).status());
        assertEquals(402, api.post("/api/v1/teams", trial.token(), map("name", "Encadeada")).status());
        CLOCK.freeze(t0.plus(14, ChronoUnit.DAYS));
        assertEquals(402, api.post("/api/v1/teams", trial.token(), map("name", "Depois do teste")).status());
        CLOCK.freeze(t0);

        Free manual = freeTeam("mchain");
        Instant end = t0.plus(1, ChronoUnit.HOURS);
        assertEquals(200, activate(manual.team().id(), "PRO", iso(end)).status());
        CLOCK.freeze(end.minusSeconds(1));
        assertEquals(200, api.post("/api/v1/teams", manual.token(), map("name", "Segunda com plano pago ativo")).status());

        Free expiring = freeTeam("mexp");
        CLOCK.freeze(t0);
        assertEquals(200, activate(expiring.team().id(), "SOLO", iso(end)).status());
        CLOCK.freeze(end);
        Resp blocked = api.post("/api/v1/teams", expiring.token(), map("name", "Depois de vencer"));
        assertEquals(402, blocked.status(), "an expired manual plan is a free team again: " + blocked.body());
    }

    @Test
    @DisplayName("UT-119 Documented: personal team and shared team each have their own trial; deleting the free team allows a new one")
    void documentedFreeTeamBehaviours() {
        Free f = freeTeam("twice");
        Resp personal = api.get("/api/v1/teams", f.token());
        long personalId = personal.at("0.personal").asBoolean() ? personal.num("0.id") : personal.num("1.id");
        assertEquals(200, api.post("/api/v1/teams/" + personalId + "/plan/trial", f.token(), map("plan", "PRO")).status());
        assertEquals(200, api.post(f.path() + "/plan/trial", f.token(), map("plan", "PRO")).status(), "decision 16: one trial per team");

        Free g = freeTeam("recreate");
        assertEquals(204, api.delete(g.path(), g.token()).status());
        Resp fresh = api.post("/api/v1/teams", g.token(), map("name", "Recriada"));
        assertEquals(200, fresh.status(), fresh.body());
        assertTrue(api.post("/api/v1/teams/" + fresh.num("id") + "/plan/trial", g.token(), map("plan", "SOLO")).status() == 200,
                "a new team has a new trial (content of the deleted team is gone)");
    }

    @Test
    @DisplayName("UT-119 (known limitation QA-M5-03, accepted and documented: not counted on admin hand-off) The rule is checked only when a team is created: handing the ADMIN role to a second account lets the first create another free team")
    void adminHandoffIsNotCounted() {
        Free f = freeTeam("handoff");
        assertEquals(200, api.post(f.path() + "/plan/trial", f.token(), map("plan", "AGENCIA")).status());
        Person puppet = person("puppet");
        long puppetMember = addMemberDirect(f.team().id(), puppet, "CREATOR");
        assertEquals(200, api.patch(f.path() + "/members/" + puppetMember, f.token(), map("role", "ADMIN")).status());
        long ownMember = 0;
        for (var m : api.get(f.path() + "/members", puppet.token()).json()) {
            if (m.path("userId").asLong() == f.admin().id()) ownMember = m.path("id").asLong();
        }
        assertEquals(200, api.patch(f.path() + "/members/" + ownMember, puppet.token(), map("role", "CREATOR")).status());
        Resp second = api.post("/api/v1/teams", f.token(), map("name", "Segunda gratuita"));
        // Today: 200. When the rule is tightened (count teams where the user was ADMIN, or block self-demotion), this becomes 402.
        assertEquals(200, second.status(), second.body());
        assertEquals(402, api.post("/api/v1/teams", f.token(), map("name", "Terceira")).status());
    }

    // --- IT-PLN-05: plan fields on the team responses ---

    @Test
    @DisplayName("IT-PLN-05 TeamResponse carries plan, planSource, planExpiresAt on list, create, rename and invite acceptance")
    void teamResponsePlanFields() {
        Instant t0 = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        CLOCK.freeze(t0);
        Person owner = person("fields");
        assertEquals(200, api.get("/api/v1/teams", owner.token()).status());
        Resp created = api.post("/api/v1/teams", owner.token(), map("name", "Campos"));
        assertEquals(200, created.status(), created.body());
        assertEquals("GRATUITO", created.text("plan"));
        assertEquals("FREE", created.text("planSource"));
        assertTrue(created.at("planExpiresAt").isNull());
        assertTrue(created.json().has("planExpiresAt"), "the key is present (null), not omitted");
        long id = created.num("id");

        assertEquals(200, api.post("/api/v1/teams/" + id + "/plan/trial", owner.token(), map("plan", "AGENCIA")).status());
        Resp renamed = api.patch("/api/v1/teams/" + id, owner.token(), map("name", "Campos 2"));
        assertEquals("AGENCIA", renamed.text("plan"));
        assertEquals("TRIAL", renamed.text("planSource"));
        assertEquals(iso(t0.plus(14, ChronoUnit.DAYS)), renamed.text("planExpiresAt"));
        Resp list = api.get("/api/v1/teams", owner.token());
        assertEquals(2, list.size());
        for (var team : list.json()) {
            if (team.path("id").asLong() == id) {
                assertEquals("AGENCIA", team.path("plan").asString());
                assertEquals("TRIAL", team.path("planSource").asString());
            } else {
                assertEquals("GRATUITO", team.path("plan").asString());
                assertTrue(team.path("personal").asBoolean());
            }
        }

        Person guest = person("guest");
        assertEquals(200, api.post("/api/v1/teams/" + id + "/invites", owner.token(), map("email", guest.email(), "role", "CREATOR")).status());
        Resp accepted = api.post("/api/v1/invites/" + MailCapture.inviteToken(guest.email()) + "/accept", guest.token(), null);
        assertEquals(200, accepted.status(), accepted.body());
        assertEquals("AGENCIA", accepted.text("plan"));
        assertEquals("TRIAL", accepted.text("planSource"));

        CLOCK.freeze(t0.plus(14, ChronoUnit.DAYS));
        Resp after = api.get("/api/v1/teams", guest.token());
        for (var team : after.json()) {
            if (team.path("id").asLong() == id) {
                assertEquals("GRATUITO", team.path("plan").asString());
                assertEquals("FREE", team.path("planSource").asString());
                assertTrue(team.path("planExpiresAt").isNull());
            }
        }
    }

    // --- DB ---

    @Test
    @DisplayName("DB-plan teams.plan / plan_source are ENUMs with defaults, expiry and trial columns are nullable TIMESTAMPs; an invalid value is refused by the database")
    void planColumns() {
        assertEquals("enum('GRATUITO','SOLO','PRO','AGENCIA')", column("plan", "COLUMN_TYPE"));
        assertEquals("GRATUITO", column("plan", "COLUMN_DEFAULT"));
        assertEquals("NO", column("plan", "IS_NULLABLE"));
        assertEquals("enum('FREE','TRIAL','MANUAL')", column("plan_source", "COLUMN_TYPE"));
        assertEquals("FREE", column("plan_source", "COLUMN_DEFAULT"));
        assertEquals("NO", column("plan_source", "IS_NULLABLE"));
        assertEquals("timestamp", column("plan_expires_at", "DATA_TYPE"));
        assertEquals("YES", column("plan_expires_at", "IS_NULLABLE"));
        assertEquals("timestamp", column("trial_used_at", "DATA_TYPE"));
        assertEquals("YES", column("trial_used_at", "IS_NULLABLE"));
        assertEquals("NO", column("personal", "IS_NULLABLE"));
        Team team = newTeam();
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE teams SET plan = 'PLATINA' WHERE id = ?", team.id()));
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE teams SET plan_source = 'GIFT' WHERE id = ?", team.id()));
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE teams SET plan = NULL WHERE id = ?", team.id()));
    }

    private String column(String name, String attribute) {
        return jdbc.queryForObject("SELECT " + attribute + " FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_NAME = 'teams' AND COLUMN_NAME = ?", String.class, DB_NAME, name);
    }

    // --- Races (acceptable at TCC scale; the numbers are printed for the report) ---

    @Test
    @DisplayName("SEC-25 Parallel requests at the plan limits: the project limit, the storage limit and the one-time trial hold (QA-M5-01)")
    void parallelRequestsAtTheLimit() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            // Projects: 2 active on Gratuito, room for exactly one more
            Free f = freeTeam("race");
            projectOk(f, "Rac 1");
            projectOk(f, "Rac 2");
            List<Callable<Integer>> creates = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                int n = i;
                creates.add(() -> project(f, "Corrida " + n).status());
            }
            List<Integer> statuses = run(pool, creates);
            long ok = statuses.stream().filter(s -> s == 200).count();
            assertTrue(statuses.stream().noneMatch(s -> s >= 500), "no 5xx: " + statuses);
            assertEquals(1, ok, "room for exactly one project: " + statuses);
            assertEquals(402, statuses.stream().filter(s -> s != 200).findFirst().orElseThrow());
            System.out.println("QA-M5 race projects: limit room 1, accepted " + ok + " of 8 -> active "
                    + plan(f).at("usage.activeProjects").asInt());

            // Storage: room for exactly two 5-byte files
            Free g = freeTeam("race2");
            long card = projectOk(g, "Material");
            insertMaterialRow(card, 100 * MB - 10);
            List<Callable<Integer>> uploads = new ArrayList<>();
            for (int i = 0; i < 8; i++) uploads.add(() -> upload(g.path(), g.token(), card, 5).status());
            List<Integer> up = run(pool, uploads);
            long accepted = up.stream().filter(s -> s == 200).count();
            assertTrue(up.stream().noneMatch(s -> s >= 500), "no 5xx: " + up);
            assertEquals(2, accepted, "room for exactly two 5-byte files: " + up);
            assertTrue(plan(g).at("usage.storageBytes").asLong() <= 100 * MB);
            System.out.println("QA-M5 race storage: room for 2, accepted " + accepted + " of 8 -> "
                    + plan(g).at("usage.storageBytes").asLong() + " bytes of " + 100 * MB);

            // Trial: a double click starts one trial; the stored plan is one of the asked ones
            Free h = freeTeam("race3");
            List<Callable<Integer>> trials = new ArrayList<>();
            for (String p : List.of("SOLO", "PRO", "AGENCIA", "SOLO", "PRO", "AGENCIA")) {
                trials.add(() -> api.post(h.path() + "/plan/trial", h.token(), map("plan", p)).status());
            }
            List<Integer> t = run(pool, trials);
            assertTrue(t.stream().noneMatch(s -> s >= 500), "no 5xx: " + t);
            assertEquals(1, t.stream().filter(s -> s == 200).count(), "one trial only: " + t);
            assertEquals(5, t.stream().filter(s -> s == 409).count(), "the others are refused: " + t);
            System.out.println("QA-M5 race trial: " + t + " -> " + dbPlan(h.team().id()));
            assertTrue(dbPlan(h.team().id()).contains("/TRIAL/"));
        } finally {
            pool.shutdownNow();
        }
    }

    private static List<Integer> run(ExecutorService pool, List<Callable<Integer>> tasks) throws Exception {
        List<Integer> out = new ArrayList<>();
        for (Future<Integer> f : pool.invokeAll(tasks)) out.add(f.get());
        return out;
    }
}
