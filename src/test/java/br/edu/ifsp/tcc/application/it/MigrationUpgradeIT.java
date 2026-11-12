package br.edu.ifsp.tcc.application.it;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DB-05 (new): upgrade of an existing database. V1..V10 are applied, legacy data is inserted (manual progress, REVIEW/DONE
 * states, free-text tags, team-less cards, orphan card, legacy modules), then V11..V16 run and the backfills are checked.
 * Uses its own scratch database (db_tcc_it_upgrade), no Spring context.
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationUpgradeIT {

    private static final String SCRATCH = "db_tcc_it_upgrade";
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "root");
    private static final String URL = "jdbc:mysql://localhost:3306/" + SCRATCH
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private Connection connection;

    private Connection connect(String url) throws SQLException {
        return DriverManager.getConnection(url, USER, PASSWORD);
    }

    private void exec(String sql) throws SQLException {
        try (Statement s = connection.createStatement()) { s.execute(sql); }
    }

    private List<List<String>> query(String sql) throws SQLException {
        List<List<String>> rows = new ArrayList<>();
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= cols; i++) row.add(rs.getString(i));
                rows.add(row);
            }
        }
        return rows;
    }

    private String one(String sql) throws SQLException {
        List<List<String>> rows = query(sql);
        return rows.isEmpty() ? null : rows.get(0).get(0);
    }

    private Flyway flyway(String target) {
        return Flyway.configure().dataSource(URL, USER, PASSWORD).locations("classpath:db/migration")
                .target(target).load();
    }

    @BeforeAll
    void createScratch() throws SQLException {
        assertEquals("db_tcc_it_upgrade", SCRATCH);
        try (Connection c = connect("jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
             Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + SCRATCH);
            s.execute("CREATE DATABASE " + SCRATCH + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        connection = connect(URL);
    }

    @AfterAll
    void dropScratch() throws SQLException {
        connection.close();
        try (Connection c = connect("jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
             Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + SCRATCH);
        }
    }

    @Test
    @DisplayName("DB-05 V11..V16 upgrade a populated V10 database: uuids, states, progress, orphan cards, tags, history")
    void upgradeKeepsData() throws SQLException {
        assertEquals(10, flyway("10").migrate().migrationsExecuted);

        exec("INSERT INTO users (id, email, password, name) VALUES (1, 'u1@creatorworkbench.test', 'x', 'Uma Pessoa'), "
                + "(2, 'u2@creatorworkbench.test', 'x', 'Dois Teste'), (3, 'u3@creatorworkbench.test', 'x', '   ')");
        exec("INSERT INTO teams (id, name) VALUES (1, 'Equipe Um')");
        exec("INSERT INTO team_members (team_id, user_id, role) VALUES (1, 1, 'ADMIN')");
        exec("INSERT INTO board_columns (id, team_id, name, sort_order) VALUES (1, 1, 'Pesquisa', 0), (2, 1, 'Roteiro', 1)");

        String past = "'2026-03-01 10:00:00'";
        // c1: project in REVIEW, manual progress 50, default tag (lowercase), column Roteiro
        exec("INSERT INTO kanban_items (id, title, state, progress, user_id, team_id, column_id, tag, created_at, updated_at) "
                + "VALUES (1, 'c1', 'REVIEW', 50, 1, 1, 2, 'tutorial', " + past + ", " + past + ")");
        // c2: project in production, no column, custom free-text tag
        exec("INSERT INTO kanban_items (id, title, state, progress, user_id, team_id, column_id, tag, created_at, updated_at) "
                + "VALUES (2, 'c2', 'IN_PRODUCTION', 10, 1, 1, NULL, '  Meu tema  ', " + past + ", " + past + ")");
        // c3: DONE card without team of a user without team -> personal team is created
        exec("INSERT INTO kanban_items (id, title, state, progress, user_id, team_id, tag, created_at, updated_at) "
                + "VALUES (3, 'c3', 'DONE', 100, 2, NULL, 'Podcast', " + past + ", " + past + ")");
        // c4: no author and no team -> deleted
        exec("INSERT INTO kanban_items (id, title, state, progress, user_id, team_id, created_at, updated_at) "
                + "VALUES (4, 'c4', 'IDEATION', 0, NULL, NULL, " + past + ", " + past + ")");
        // c5: idea of a team member with a blank tag; c6: team-less idea of a user that has a team
        exec("INSERT INTO kanban_items (id, title, state, progress, user_id, team_id, tag, created_at, updated_at) "
                + "VALUES (5, 'c5', 'IDEATION', 0, 1, 1, '   ', " + past + ", " + past + ")");
        exec("INSERT INTO kanban_items (id, title, state, progress, user_id, team_id, created_at, updated_at) "
                + "VALUES (6, 'c6', 'IDEATION', 0, 1, NULL, " + past + ", " + past + ")");
        exec("INSERT INTO scripts (kanban_item_id, content) VALUES (1, '<p>roteiro</p>')");
        exec("INSERT INTO card_tasks (kanban_item_id, title) VALUES (1, 'tarefa')");
        exec("INSERT INTO modules (id, kanban_item_id, title) VALUES (1, 1, 'modulo legado')");
        exec("INSERT INTO lessons (module_id, title) VALUES (1, 'aula legada')");

        assertEquals(6, flyway("16").migrate().migrationsExecuted);
        assertEquals("16", one("SELECT MAX(CAST(version AS UNSIGNED)) FROM flyway_schema_history"));

        // RN05: every row has a distinct uuid
        assertEquals("0", one("SELECT COUNT(*) FROM kanban_items WHERE uuid IS NULL OR uuid = ''"));
        assertEquals(one("SELECT COUNT(*) FROM kanban_items"), one("SELECT COUNT(DISTINCT uuid) FROM kanban_items"));
        assertTrue(one("SELECT uuid FROM scripts LIMIT 1").matches("[0-9a-f-]{36}"));

        // progress column and legacy tables gone; REVIEW/DONE became IN_PRODUCTION
        assertEquals("0", one("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = '" + SCRATCH
                + "' AND TABLE_NAME = 'kanban_items' AND COLUMN_NAME IN ('progress', 'tag')"));
        assertEquals("0", one("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + SCRATCH
                + "' AND TABLE_NAME IN ('modules', 'lessons')"));
        assertEquals("IN_PRODUCTION", one("SELECT state FROM kanban_items WHERE id = 1"));
        assertEquals("IN_PRODUCTION", one("SELECT state FROM kanban_items WHERE id = 3"));
        assertEquals("IDEATION", one("SELECT state FROM kanban_items WHERE id = 5"));

        // c4 (no author, no team) is deleted; everything else belongs to a team
        assertNull(one("SELECT id FROM kanban_items WHERE id = 4"));
        assertEquals("0", one("SELECT COUNT(*) FROM kanban_items WHERE team_id IS NULL"));
        assertEquals("1", one("SELECT team_id FROM kanban_items WHERE id = 6"), "team-less idea goes to the author's team");

        // user 2 got a personal team with the 4 default columns, 5 default tags and is ADMIN; c3 sits in its first column
        String personal = one("SELECT team_id FROM kanban_items WHERE id = 3");
        assertNotEquals("1", personal);
        assertEquals("Estúdio de Dois", one("SELECT name FROM teams WHERE id = " + personal));
        assertEquals("ADMIN", one("SELECT role FROM team_members WHERE team_id = " + personal + " AND user_id = 2"));
        assertEquals("Pesquisa,Roteiro,Gravação,Edição", one("SELECT GROUP_CONCAT(name ORDER BY sort_order) FROM board_columns WHERE team_id = " + personal));
        assertEquals("5", one("SELECT COUNT(*) FROM tags WHERE team_id = " + personal));
        assertEquals(one("SELECT id FROM board_columns WHERE team_id = " + personal + " AND sort_order = 0"),
                one("SELECT column_id FROM kanban_items WHERE id = 3"));
        assertEquals("Podcast", one("SELECT t.name FROM kanban_items k JOIN tags t ON t.id = k.tag_id WHERE k.id = 3"));
        assertNull(one("SELECT id FROM teams WHERE name = 'Meu Estudio'"), "user 3 owns no card, so no team is created");

        // c2 without column goes to the first column of its team
        assertEquals("1", one("SELECT column_id FROM kanban_items WHERE id = 2"));

        // tags: default matched case-insensitively, custom tag created once with the neutral color, blank means none
        assertEquals("Tutorial", one("SELECT t.name FROM kanban_items k JOIN tags t ON t.id = k.tag_id WHERE k.id = 1"));
        assertEquals("Meu tema", one("SELECT t.name FROM kanban_items k JOIN tags t ON t.id = k.tag_id WHERE k.id = 2"));
        assertEquals("#607D8B", one("SELECT t.color FROM kanban_items k JOIN tags t ON t.id = k.tag_id WHERE k.id = 2"));
        assertNull(one("SELECT tag_id FROM kanban_items WHERE id = 5"));
        assertEquals("6", one("SELECT COUNT(*) FROM tags WHERE team_id = 1"), "5 defaults + 1 custom");

        // history: one row per project card, none for ideas, entered at the card's last update (untouched by the migrations)
        assertEquals("3", one("SELECT COUNT(*) FROM card_movements"));
        assertEquals("0", one("SELECT COUNT(*) FROM card_movements WHERE kanban_item_id IN (5, 6)"));
        assertEquals("Roteiro", one("SELECT column_name FROM card_movements WHERE kanban_item_id = 1"));
        assertEquals("Pesquisa", one("SELECT column_name FROM card_movements WHERE kanban_item_id = 2"));
        assertEquals("2026-03-01 10:00:00", one("SELECT DATE_FORMAT(entered_at, '%Y-%m-%d %H:%i:%s') FROM card_movements WHERE kanban_item_id = 1"));
        assertEquals("2026-03-01 10:00:00", one("SELECT DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') FROM kanban_items WHERE id = 1"),
                "the back-fills must not bump updated_at");
        assertEquals("1", one("SELECT COUNT(*) FROM card_movements WHERE moved_by IS NULL AND kanban_item_id = 1"));

        // untouched content survives
        assertEquals("<p>roteiro</p>", one("SELECT content FROM scripts WHERE kanban_item_id = 1"));
        assertEquals("1", one("SELECT COUNT(*) FROM card_tasks WHERE kanban_item_id = 1"));
        assertNull(one("SELECT assignee_member_id FROM card_tasks WHERE kanban_item_id = 1"));
    }
}
