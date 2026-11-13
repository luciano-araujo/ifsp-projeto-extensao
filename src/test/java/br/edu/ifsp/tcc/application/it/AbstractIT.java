package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base of the integration tests: the whole application on a random port, talking to the LOCAL MySQL database
 * {@value #DB_NAME}. The database is dropped and rebuilt (Flyway V1..latest from an empty schema) once per JVM,
 * before the first Spring context starts. Each test class gets its own users and teams.
 * Only the database {@value #DB_NAME} is ever touched; db_tcc (demo data) is never used.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("it")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIT {

    /** db_tcc_it by default; parallel slices may use IT_DB_NAME=db_tcc_it_<suffix> (only db_tcc_it* names are allowed). */
    public static final String DB_NAME = itDbName();
    public static final String PASSWORD = "Teste@123";

    static {
        // Same as Application.main: UTC must be set before the first JDBC connection (the driver caches the zone)
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        resetDatabase();
    }

    @Autowired protected Environment environment;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JdbcTemplate jdbc;

    protected Api api;
    protected Person ana;      // ADMIN of every team built by newTeam()
    protected Person carlos;   // CREATOR
    protected Person rui;      // REVIEWER
    protected Person bia;      // second ADMIN-less outsider with a team of her own (team B)
    protected Person loner;    // registered user that belongs to no team
    protected Team otherTeam;  // team B, owned by bia

    /** A user with a real JWT obtained through POST /api/auth/login. */
    public record Person(long id, String email, String token) { }

    /** A team with one member per role and its default columns and tags. */
    public record Team(long id, long adminMemberId, long creatorMemberId, long reviewerMemberId,
                       List<Long> columnIds, List<Long> tagIds) {
        public String path() { return "/api/v1/teams/" + id; }
    }

    private static String itDbName() {
        String name = System.getenv().getOrDefault("IT_DB_NAME", "db_tcc_it");
        if (!name.matches("db_tcc_it(_[a-z0-9]{1,20})?")) throw new IllegalStateException("IT_DB_NAME must be db_tcc_it or db_tcc_it_<suffix>, got: " + name);
        return name;
    }

    private static void resetDatabase() {
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "root");
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", user, password);
             Statement statement = connection.createStatement()) {
            if (!DB_NAME.matches("db_tcc_it(_[a-z0-9]{1,20})?")) throw new IllegalStateException("Refusing to reset " + DB_NAME);
            statement.execute("DROP DATABASE IF EXISTS " + DB_NAME);
            statement.execute("CREATE DATABASE " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot reset " + DB_NAME + " (is MySQL up and DB_PASSWORD set?)", e);
        }
    }

    @BeforeAll
    void buildScenario() {
        String url = environment.getProperty("spring.datasource.url");
        if (url == null || !url.contains("/" + DB_NAME + "?")) {
            throw new IllegalStateException("Integration tests must run on " + DB_NAME + ", got: " + url);
        }
        api = new Api(Integer.parseInt(environment.getProperty("local.server.port")));
        ana = person("ana");
        carlos = person("carlos");
        rui = person("rui");
        bia = person("bia");
        loner = person("loner");
        otherTeam = newTeamFor(bia, "Equipe B", null, null);
    }

    // --- Fixtures ---

    /** Creates a user straight in the database (no e-mail involved) and logs in through the real auth route. */
    protected Person person(String prefix) {
        String email = prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@creatorworkbench.test";
        User user = new User();
        user.setEmail(email);
        user.setName(prefix.substring(0, 1).toUpperCase() + prefix.substring(1) + " Teste");
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user = userRepository.save(user);
        Resp login = api.post("/api/auth/login", null, map("email", email, "password", PASSWORD));
        assertEquals(200, login.status(), "login of the fixture user: " + login.body());
        return new Person(user.getId(), email, login.text("token"));
    }

    /** New team: ana ADMIN, carlos CREATOR, rui REVIEWER. */
    protected Team newTeam() {
        return newTeamFor(ana, "Equipe " + UUID.randomUUID().toString().substring(0, 6), carlos, rui);
    }

    protected Team newTeamFor(Person admin, String name, Person creator, Person reviewer) {
        // Plans: the scenarios build teams with several members and many projects, so the fixture teams run on
        // the top plan (set straight in the database). Existing teams of the admin are upgraded first because a
        // person may administer only one team on Gratuito; PlanIT checks the real limits on Gratuito teams.
        jdbc.update("UPDATE teams SET plan = 'AGENCIA', plan_source = 'MANUAL', plan_expires_at = NULL WHERE id IN "
                + "(SELECT team_id FROM team_members WHERE user_id = ? AND role = 'ADMIN')", admin.id());
        Resp created = api.post("/api/v1/teams", admin.token(), map("name", name));
        assertEquals(200, created.status(), created.body());
        long teamId = created.num("id");
        setPlan(teamId, "AGENCIA");
        long adminMemberId = membersOf(teamId, admin).stream().findFirst().orElseThrow();
        long creatorMemberId = 0;
        long reviewerMemberId = 0;
        if (creator != null) {
            creatorMemberId = addMemberDirect(teamId, creator, "CREATOR");
        }
        if (reviewer != null) {
            reviewerMemberId = addMemberDirect(teamId, reviewer, "REVIEWER");
        }
        Resp board = api.get("/api/v1/teams/" + teamId + "/board", admin.token());
        List<Long> columns = new ArrayList<>();
        board.at("columns").forEach(c -> columns.add(c.path("id").asLong()));
        Resp tags = api.get("/api/v1/teams/" + teamId + "/tags", admin.token());
        List<Long> tagIds = new ArrayList<>();
        tags.json().forEach(t -> tagIds.add(t.path("id").asLong()));
        return new Team(teamId, adminMemberId, creatorMemberId, reviewerMemberId, columns, tagIds);
    }

    /**
     * Adds a membership straight in the IT database and returns the member id. Fixture choice (decision 22): the route
     * POST /teams/{t}/members now creates an invite (the user must accept), so fixtures cannot use it to build teams.
     * SQL is faster than invite + accept through the API and is not subject to plan seat limits, like the other
     * fixture shortcuts (setPlan). The invite flow itself is covered by InvitesIT, SecurityIT and the plan ITs.
     */
    protected long addMemberDirect(long teamId, Person who, String role) {
        jdbc.update("INSERT INTO team_members (team_id, user_id, role) VALUES (?, ?, ?)", teamId, who.id(), role);
        return jdbc.queryForObject("SELECT id FROM team_members WHERE team_id = ? AND user_id = ?", Long.class,
                teamId, who.id());
    }

    /** Sets the plan of a team straight in the database (a manual activation without expiry). */
    protected void setPlan(long teamId, String plan) {
        jdbc.update("UPDATE teams SET plan = ?, plan_source = ?, plan_expires_at = NULL WHERE id = ?",
                plan, "GRATUITO".equals(plan) ? "FREE" : "MANUAL", teamId);
    }

    private List<Long> membersOf(long teamId, Person who) {
        Resp members = api.get("/api/v1/teams/" + teamId + "/members", who.token());
        List<Long> ids = new ArrayList<>();
        members.json().forEach(m -> ids.add(m.path("id").asLong()));
        return ids;
    }

    protected Resp createIdea(Team team, Person by, String title) {
        return api.post(team.path() + "/ideas", by.token(), map("title", title, "description", "Descricao de " + title));
    }

    protected long ideaId(Team team, String title) {
        Resp r = createIdea(team, ana, title);
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    protected Resp createProject(Team team, Person by, String title, Long columnId) {
        return api.post(team.path() + "/cards", by.token(), map("title", title, "description", "d",
                "targetAudience", "Estudantes iniciantes", "pedagogicalObjective", "Aprender o basico do tema",
                "columnId", columnId));
    }

    protected long projectId(Team team, String title) {
        Resp r = createProject(team, ana, title, team.columnIds().get(0));
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }

    protected long taskId(Team team, long cardId, String title) {
        Resp r = api.post(team.path() + "/cards/" + cardId + "/tasks", ana.token(), map("title", title));
        assertEquals(200, r.status(), r.body());
        return r.num("id");
    }
}
