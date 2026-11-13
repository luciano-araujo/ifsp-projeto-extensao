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
 * Backfill of teams.personal (V25) on an existing database: V1..V10 are applied, teams and members are inserted, then every
 * migration runs. Own scratch database (db_tcc_it_backfill), no Spring context.
 */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonalFlagBackfillIT {

    private static final String SCRATCH = "db_tcc_it_backfill";
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "root");
    private static final String SERVER = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String URL = "jdbc:mysql://localhost:3306/" + SCRATCH
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private Connection connection;

    private void exec(String sql) throws SQLException {
        try (Statement s = connection.createStatement()) { s.execute(sql); }
    }

    private List<Long> flaggedTeams() throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery("SELECT id FROM teams WHERE personal = TRUE ORDER BY id")) {
            while (rs.next()) ids.add(rs.getLong(1));
        }
        return ids;
    }

    @BeforeAll
    void createScratch() throws SQLException {
        try (Connection c = DriverManager.getConnection(SERVER, USER, PASSWORD); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + SCRATCH);
            s.execute("CREATE DATABASE " + SCRATCH + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    @AfterAll
    void dropScratch() throws SQLException {
        connection.close();
        try (Connection c = DriverManager.getConnection(SERVER, USER, PASSWORD); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS " + SCRATCH);
        }
    }

    @Test
    @DisplayName("DB-06 V25 flags the real personal teams (also when a shared team was joined first) and no shared team")
    void backfill() throws SQLException {
        Flyway.configure().dataSource(URL, USER, PASSWORD).locations("classpath:db/migration").target("10").load().migrate();

        exec("INSERT INTO users (id, email, password, name) VALUES "
                + "(1, 'ana@creatorworkbench.test', 'x', 'Ana Souza'), (2, 'bia@creatorworkbench.test', 'x', 'Bia Lima'), "
                + "(3, 'blank@creatorworkbench.test', 'x', '   '), (4, 'dani@creatorworkbench.test', 'x', 'Dani'), "
                + "(5, 'eva@creatorworkbench.test', 'x', 'Eva')");
        exec("INSERT INTO teams (id, name) VALUES (1, 'Equipe Alfa'), (2, 'Estúdio de Ana'), (4, 'Estúdio de Bia'), "
                + "(5, 'Meu Estúdio'), (6, 'Meu canal'), (7, 'Estúdio de Eva'), (8, 'Equipe Beta'), (9, 'Estúdio de Carlos')");
        // Team 1 is shared and was joined by Bia before her personal team 4 (which has a higher id)
        exec("INSERT INTO team_members (team_id, user_id, role, joined_at) VALUES "
                + "(1, 1, 'ADMIN', '2026-01-01 10:00:00'), (1, 2, 'CREATOR', '2026-01-02 10:00:00'), "
                + "(2, 1, 'ADMIN', '2026-01-03 10:00:00'), "
                + "(4, 2, 'ADMIN', '2026-02-01 10:00:00'), "
                + "(5, 3, 'ADMIN', '2026-01-05 10:00:00'), "
                // renamed personal team of Dani: not recognisable by name, not flagged
                + "(6, 4, 'ADMIN', '2026-01-06 10:00:00'), "
                + "(7, 5, 'ADMIN', '2026-01-07 10:00:00'), (8, 5, 'ADMIN', '2026-01-08 10:00:00'), "
                // a shared team named like a personal one, administered by someone else's first team owner
                + "(9, 1, 'ADMIN', '2026-03-01 10:00:00')");

        Flyway.configure().dataSource(URL, USER, PASSWORD).locations("classpath:db/migration").load().migrate();

        assertEquals(List.of(2L, 4L, 5L, 7L), flaggedTeams(),
                "personal: Ana (2), Bia (4, although she joined team 1 first), blank name (5), Eva (7)");
    }
}
