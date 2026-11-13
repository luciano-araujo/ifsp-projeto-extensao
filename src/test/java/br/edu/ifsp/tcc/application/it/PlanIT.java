package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Plans: limits of Gratuito (HTTP 402), trial, manual activation by the system administrator and the plan routes.
 * The other ITs run their teams on AGENCIA (see AbstractIT.newTeamFor); here the teams are real Gratuito teams.
 */
class PlanIT extends AbstractIT {

    static final String SYSADMIN_EMAIL = "sysadmin.m5@creatorworkbench.test";
    private static final long MB = 1024L * 1024;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** A fresh user with one real team on Gratuito (created through POST /teams). */
    private record Free(Person admin, Team team) { }

    private Free freeTeam() {
        Person admin = person("gina");
        // The personal team (auto-created by the first GET /teams) does not count as the one free team
        Resp personal = api.get("/api/v1/teams", admin.token());
        assertEquals(1, personal.size(), personal.body());
        assertEquals("Estúdio de Gina", personal.text("0.name"));
        Resp created = api.post("/api/v1/teams", admin.token(), map("name", "Gratuita " + UUID.randomUUID().toString().substring(0, 5)));
        assertEquals(200, created.status(), created.body());
        assertEquals("GRATUITO", created.text("plan"));
        assertEquals("FREE", created.text("planSource"));
        long id = created.num("id");
        Resp board = api.get("/api/v1/teams/" + id + "/board", admin.token());
        java.util.List<Long> columns = new java.util.ArrayList<>();
        board.at("columns").forEach(c -> columns.add(c.path("id").asLong()));
        return new Free(admin, new Team(id, 0, 0, 0, columns, java.util.List.of()));
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
        if (userRepository.findByEmail(SYSADMIN_EMAIL).isEmpty()) {
            User user = new User();
            user.setEmail(SYSADMIN_EMAIL);
            user.setName("Admin Sistema");
            user.setPassword(passwordEncoder.encode(PASSWORD));
            userRepository.save(user);
        }
        Resp login = api.post("/api/auth/login", null, map("email", SYSADMIN_EMAIL, "password", PASSWORD));
        assertEquals(200, login.status(), login.body());
        return new Person(0, SYSADMIN_EMAIL, login.text("token"));
    }

    private void expireNow(long teamId) {
        jdbc.update("UPDATE teams SET plan_expires_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 MINUTE) WHERE id = ?", teamId);
    }

    private Resp uploadPdf(Free f, long cardId, int size) {
        String boundary = "----cw" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] content = new byte[size];
        java.util.Arrays.fill(content, (byte) 'a');
        try {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(content);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:"
                            + environment.getProperty("local.server.port") + f.team().path() + "/cards/" + cardId + "/materials"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Authorization", "Bearer " + f.admin().token())
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Resp(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- Catalogue and team plan ---

    @Test
    @DisplayName("IT-PLAN-01 GET /plans lists the four plans with prices and limits (null = unlimited)")
    void catalogue() {
        Resp r = api.get("/api/v1/plans", ana.token());
        assertEquals(200, r.status(), r.body());
        assertEquals(4, r.size());
        assertEquals("GRATUITO", r.text("0.plan"));
        assertEquals(1, r.at("0.limits.maxUsers").asInt());
        assertEquals(3, r.at("0.limits.maxActiveProjects").asInt());
        assertEquals(1, r.at("0.limits.maxCourses").asInt());
        assertFalse(r.at("0.limits.automaticSummary").asBoolean());
        assertEquals(100 * MB, r.at("0.limits.storageBytes").asLong());
        assertEquals(49.0, r.at("1.monthlyPrice").asDouble());
        assertTrue(r.at("2.limits.maxActiveProjects").isNull());
        assertEquals("Agência", r.text("3.name"));
        assertEquals(5, r.at("3.limits.maxUsers").asInt());
        assertEquals(401, api.get("/api/v1/plans", null).status());
    }

    @Test
    @DisplayName("IT-PLAN-02 GET team plan: any member sees plan, limits and usage; outsiders get 403")
    void teamPlan() {
        Free f = freeTeam();
        projectOk(f, "Um projeto");
        Resp r = api.get(f.team().path() + "/plan", f.admin().token());
        assertEquals(200, r.status(), r.body());
        assertEquals("GRATUITO", r.text("plan"));
        assertEquals("Gratuito", r.text("planName"));
        assertEquals("FREE", r.text("source"));
        assertTrue(r.at("trialAvailable").asBoolean());
        assertEquals(1, r.at("usage.members").asInt());
        assertEquals(1, r.at("usage.activeProjects").asInt());
        assertEquals(0, r.at("usage.courses").asInt());
        assertFalse(r.at("overLimit").asBoolean());
        assertEquals(3, r.at("limits.maxActiveProjects").asInt());

        Resp viaTeams = api.get("/api/v1/teams", f.admin().token());
        assertEquals("GRATUITO", viaTeams.text("0.plan"));
        assertEquals(403, api.get(f.team().path() + "/plan", ana.token()).status());
    }

    // --- 402 ---

    @Test
    @DisplayName("IT-PLAN-03 Gratuito: no extra members (402) and one free team per administrator")
    void membersAndFreeTeam() {
        Free f = freeTeam();
        Resp add = api.post(f.team().path() + "/members", f.admin().token(), map("email", carlos.email(), "role", "CREATOR"));
        assertEquals(402, add.status(), add.body());
        assertTrue(add.text("message").contains("plano Gratuito"), add.body());
        assertTrue(add.text("message").contains("Agência"), add.body());

        Resp second = api.post("/api/v1/teams", f.admin().token(), map("name", "Segunda gratuita"));
        assertEquals(402, second.status(), second.body());
        assertEquals(2, api.get("/api/v1/teams", f.admin().token()).size(), "personal team + one shared free team");

        setPlan(f.team().id(), "PRO");
        assertEquals(200, api.post("/api/v1/teams", f.admin().token(), map("name", "Segunda equipe")).status());
        setPlan(f.team().id(), "AGENCIA");
        assertEquals(200, api.post(f.team().path() + "/members", f.admin().token(),
                map("email", carlos.email(), "role", "CREATOR")).status());
    }

    @Test
    @DisplayName("IT-PLAN-04 Gratuito: 3 active projects; the 4th is 402; approved ones stop counting; ideas convert under the same limit")
    void activeProjects() {
        Free f = freeTeam();
        long first = projectOk(f, "Projeto um");
        projectOk(f, "Projeto dois");
        projectOk(f, "Projeto tres");
        long idea = api.post(f.team().path() + "/ideas", f.admin().token(), map("title", "Uma ideia", "description", "d")).num("id");

        Resp fourth = project(f, "Projeto quatro");
        assertEquals(402, fourth.status(), fourth.body());
        assertTrue(fourth.text("message").contains("até 3 projetos ativos"), fourth.body());
        assertTrue(fourth.text("message").contains("plano Solo"), fourth.body());
        Resp convert = api.post(f.team().path() + "/cards/" + idea + "/convert", f.admin().token(),
                map("title", "Ideia convertida", "targetAudience", "Estudantes", "pedagogicalObjective", "Ensinar algo importante",
                        "columnId", f.team().columnIds().get(0)));
        assertEquals(402, convert.status(), convert.body());

        // Approving a project frees a slot
        assertEquals(200, api.post(f.team().path() + "/cards/" + first + "/reviews", f.admin().token(),
                map("decision", "APPROVED")).status());
        assertEquals(200, project(f, "Projeto quatro").status());
        assertEquals(402, project(f, "Projeto cinco").status());
        // ...until a new review asks for changes (the latest review decides)
        assertEquals(200, api.post(f.team().path() + "/cards/" + first + "/reviews", f.admin().token(),
                map("decision", "CHANGES_REQUESTED", "comment", "Ajustar")).status());
        Resp plan = api.get(f.team().path() + "/plan", f.admin().token());
        assertEquals(4, plan.at("usage.activeProjects").asInt());
        assertTrue(plan.at("overLimit").asBoolean());
        // Over the limit nothing is deleted or blocked except new creation
        assertEquals(200, api.get(f.team().path() + "/board", f.admin().token()).status());
        assertEquals(200, api.put("/api/v1/kanban/" + first, f.admin().token(), map("title", "Projeto um editado")).status());
    }

    @Test
    @DisplayName("IT-PLAN-05 Gratuito: 1 course; the 2nd is 402; Solo allows more")
    void courses() {
        Free f = freeTeam();
        assertEquals(200, api.post(f.team().path() + "/courses", f.admin().token(), map("title", "Curso 1")).status());
        Resp second = api.post(f.team().path() + "/courses", f.admin().token(), map("title", "Curso 2"));
        assertEquals(402, second.status(), second.body());
        assertTrue(second.text("message").contains("até 1 curso"), second.body());
        setPlan(f.team().id(), "SOLO");
        assertEquals(200, api.post(f.team().path() + "/courses", f.admin().token(), map("title", "Curso 2")).status());
    }

    @Test
    @DisplayName("IT-PLAN-06 Automatic summary is 402 on Gratuito and Solo and allowed on Pro")
    void summary() {
        Free f = freeTeam();
        long card = projectOk(f, "Com roteiro");
        String path = f.team().path() + "/cards/" + card + "/summary";
        Resp gratuito = api.post(path, f.admin().token(), null);
        assertEquals(402, gratuito.status(), gratuito.body());
        assertTrue(gratuito.text("message").contains("plano Pro"), gratuito.body());
        setPlan(f.team().id(), "SOLO");
        assertEquals(402, api.post(path, f.admin().token(), null).status());
        setPlan(f.team().id(), "PRO");
        // Past the plan gate: the empty script is now refused by the 100-word rule (400), not by the plan
        assertEquals(400, api.post(path, f.admin().token(), null).status());
    }

    @Test
    @DisplayName("IT-PLAN-07 Materials: 100 MB in total on Gratuito; a file that fits is accepted, one byte over is 402")
    void storage() {
        Free f = freeTeam();
        long card = projectOk(f, "Com material");
        jdbc.update("INSERT INTO materials (uuid, kanban_item_id, file_name, content_type, size_bytes, storage_key) "
                + "VALUES (?, ?, 'grande.pdf', 'application/pdf', ?, ?)", UUID.randomUUID().toString(), card,
                100 * MB - 10, UUID.randomUUID().toString());

        assertEquals(200, uploadPdf(f, card, 10).status());
        Resp over = uploadPdf(f, card, 1);
        assertEquals(402, over.status(), over.body());
        assertTrue(over.text("message").contains("100 MB"), over.body());
        assertTrue(over.text("message").contains("plano Solo"), over.body());
        assertEquals(100 * MB, api.get(f.team().path() + "/plan", f.admin().token()).at("usage.storageBytes").asLong());

        setPlan(f.team().id(), "SOLO");
        assertEquals(200, uploadPdf(f, card, 1).status());
    }

    // --- Trial ---

    @Test
    @DisplayName("IT-PLAN-08 Trial: ADMIN only, 14 days, once per team, then the team reverts to Gratuito")
    void trial() {
        Free f = freeTeam();
        String path = f.team().path() + "/plan/trial";
        assertEquals(400, api.post(path, f.admin().token(), map("plan", "GRATUITO")).status());
        assertEquals(400, api.post(path, f.admin().token(), map()).status());
        assertEquals(403, api.post(path, ana.token(), map("plan", "PRO")).status());

        Resp started = api.post(path, f.admin().token(), map("plan", "PRO"));
        assertEquals(200, started.status(), started.body());
        assertEquals("PRO", started.text("plan"));
        assertEquals("TRIAL", started.text("source"));
        assertFalse(started.at("trialAvailable").asBoolean());
        java.time.LocalDateTime expires = java.time.LocalDateTime.parse(started.text("expiresAt").replace("Z", ""));
        long days = java.time.Duration.between(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC), expires).toHours();
        assertTrue(days >= 14 * 24 - 1 && days <= 14 * 24, "about 14 days, was " + days + " hours");

        // During the trial Pro limits apply (unlimited projects)
        for (int i = 1; i <= 4; i++) projectOk(f, "Durante o teste " + i);
        assertEquals("PRO", api.get("/api/v1/teams", f.admin().token()).text("1.plan"));
        assertEquals(409, api.post(path, f.admin().token(), map("plan", "SOLO")).status());

        // Expired: computed on read, no job
        expireNow(f.team().id());
        Resp after = api.get(f.team().path() + "/plan", f.admin().token());
        assertEquals("GRATUITO", after.text("plan"));
        assertEquals("FREE", after.text("source"));
        assertTrue(after.at("expiresAt").isNull());
        assertTrue(after.at("overLimit").asBoolean());
        assertEquals(402, project(f, "Depois do teste").status());
        assertEquals("GRATUITO", api.get("/api/v1/teams", f.admin().token()).text("1.plan"));
        Resp again = api.post(path, f.admin().token(), map("plan", "SOLO"));
        assertEquals(409, again.status(), again.body());
        assertEquals("O teste grátis já foi usado por esta equipe.", again.text("message"));
    }

    // --- System administrator ---

    @Test
    @DisplayName("IT-PLAN-09 Admin: PUT /admin/teams/{t}/plan activates a plan; GET /admin/teams?email= finds teams; others get 403")
    void admin() {
        Free f = freeTeam();
        Person sys = sysadmin();
        String path = "/api/v1/admin/teams/" + f.team().id() + "/plan";

        assertEquals(403, api.put(path, f.admin().token(), map("plan", "PRO")).status());
        assertEquals(403, api.get("/api/v1/admin/teams?email=" + f.admin().email(), f.admin().token()).status());
        assertEquals(401, api.put(path, null, map("plan", "PRO")).status());

        assertEquals(400, api.put(path, sys.token(), map()).status());
        assertEquals(400, api.put(path, sys.token(), map("plan", "PLATINA")).status());
        assertEquals(404, api.put("/api/v1/admin/teams/999999999/plan", sys.token(), map("plan", "PRO")).status());
        Resp past = api.put(path, sys.token(), map("plan", "PRO", "expiresAt", "2020-01-01T00:00:00Z"));
        assertEquals(400, past.status(), past.body());

        assertEquals(400, api.put(path, sys.token(), map("plan", "PRO", "expiresAt", "2099-01-01T00:00:00Z")).status());
        Resp activated = api.put(path, sys.token(), map("plan", "AGENCIA", "expiresAt", "2037-12-31T23:59:59Z"));
        assertEquals(200, activated.status(), activated.body());
        assertEquals("AGENCIA", activated.text("plan"));
        assertEquals("MANUAL", activated.text("source"));
        assertEquals("2037-12-31T23:59:59Z", activated.text("expiresAt"));
        assertTrue(activated.at("trialAvailable").asBoolean(), "a manual activation does not use the trial");
        assertEquals(5, activated.at("limits.maxUsers").asInt());
        assertEquals(200, api.post(f.team().path() + "/members", f.admin().token(),
                map("email", carlos.email(), "role", "CREATOR")).status());

        Resp byEmail = api.get("/api/v1/admin/teams?email=" + f.admin().email(), sys.token());
        assertEquals(2, byEmail.size(), "personal team + shared team");
        assertEquals(f.team().id(), byEmail.at("1.id").asLong());
        assertEquals("AGENCIA", byEmail.text("1.plan"));
        assertEquals("MANUAL", byEmail.text("1.planSource"));
        assertEquals(2, byEmail.at("1.membersCount").asInt());
        assertEquals("GRATUITO", byEmail.text("0.plan"));
        assertEquals(0, api.get("/api/v1/admin/teams?email=ninguem@creatorworkbench.test", sys.token()).size());
        assertEquals(400, api.get("/api/v1/admin/teams", sys.token()).status());

        Resp reset = api.put(path, sys.token(), map("plan", "GRATUITO"));
        assertEquals(200, reset.status(), reset.body());
        assertEquals("GRATUITO", reset.text("plan"));
        assertEquals("FREE", reset.text("source"));

        // An activation with an end date also falls back to Gratuito on read
        assertEquals(200, api.put(path, sys.token(), map("plan", "PRO", "expiresAt", "2037-01-01T00:00:00Z")).status());
        expireNow(f.team().id());
        assertEquals("GRATUITO", api.get(f.team().path() + "/plan", f.admin().token()).text("plan"));
    }

    @Test
    @DisplayName("IT-PLAN-10 Existing teams get Gratuito from the migration default")
    void migrationDefault() {
        Team team = newTeam();
        jdbc.update("UPDATE teams SET plan = DEFAULT, plan_source = DEFAULT, plan_expires_at = NULL, trial_used_at = NULL WHERE id = ?", team.id());
        assertEquals("GRATUITO", jdbc.queryForObject("SELECT plan FROM teams WHERE id = ?", String.class, team.id()));
        assertEquals("FREE", jdbc.queryForObject("SELECT plan_source FROM teams WHERE id = ?", String.class, team.id()));
        setPlan(team.id(), "AGENCIA");
    }
}
