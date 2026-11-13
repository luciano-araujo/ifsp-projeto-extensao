package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.io.IOException;
import java.util.Comparator;
import java.util.ArrayList;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.Resource;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Plan DB-01..DB-04 (updated for V1-V17) and the schema facts M1 relies on. */
class DatabaseSchemaIT extends AbstractIT {

    @Autowired ApplicationContext context;

    private Integer count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    private String columnType(String table, String column) {
        return jdbc.queryForObject("SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", String.class, DB_NAME, table, column);
    }

    private String nullable(String table, String column) {
        return jdbc.queryForObject("SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_NAME = ? AND COLUMN_NAME = ?", String.class, DB_NAME, table, column);
    }

    @Test
    @DisplayName("DB-01 Flyway builds every migration V1..Vn, with no gaps, from an empty database and Hibernate ddl-auto=validate accepts it")
    void flywayCleanBuild() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank");
        // Every migration file of the project, in version order; they must be exactly V1..Vn
        List<String> expected = new ArrayList<>();
        try {
            for (Resource file : new PathMatchingResourcePatternResolver()
                    .getResources("classpath:db/migration/V*__*.sql")) {
                String name = file.getFilename();
                expected.add(name.substring(1, name.indexOf("__")));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        expected.sort(Comparator.comparingInt(Integer::parseInt));
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(String.valueOf(i + 1), expected.get(i), "migration versions must be contiguous: " + expected);
        }
        assertTrue(expected.size() >= 21, "expected at least V1..V21: " + expected);
        assertEquals(expected.size(), history.size(), "migrations applied: " + history);
        for (int i = 0; i < history.size(); i++) {
            assertEquals(expected.get(i), history.get(i).get("version"));
            Object success = history.get(i).get("success");
            assertTrue(Boolean.TRUE.equals(success) || (success instanceof Number n && n.intValue() == 1),
                    "V" + expected.get(i) + " failed");
        }
        // The context only starts when Hibernate validates every entity against the migrated schema
        assertNotNull(context);
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals(DB_NAME, jdbc.queryForObject("SELECT DATABASE()", String.class));
    }

    @Test
    @DisplayName("DB-01c M2 materials schema: materials and course_lesson_materials with their keys and cascade rules")
    void materialsSchemaShape() {
        assertEquals("char(36)", columnType("materials", "uuid"));
        assertEquals("NO", nullable("materials", "kanban_item_id"));
        assertEquals("YES", nullable("materials", "uploaded_by"));
        assertEquals("YES", nullable("materials", "archived_at"));
        assertEquals("NO", nullable("materials", "storage_key"));
        assertEquals("bigint", columnType("materials", "size_bytes"));
        for (String column : List.of("uuid", "storage_key")) {
            assertEquals(1, count("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? "
                    + "AND TABLE_NAME = 'materials' AND COLUMN_NAME = ? AND NON_UNIQUE = 0", DB_NAME, column),
                    "materials." + column + " must be unique");
        }
        assertEquals("CASCADE", deleteRule("fk_materials_kanban_item"));
        assertEquals("SET NULL", deleteRule("fk_materials_uploaded_by"));
        assertEquals("CASCADE", deleteRule("fk_course_lesson_materials_lesson"));
        assertEquals("CASCADE", deleteRule("fk_course_lesson_materials_material"));
        assertEquals(1, count("SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_NAME = 'course_lesson_materials' AND CONSTRAINT_NAME = 'uk_course_lesson_materials' "
                + "AND CONSTRAINT_TYPE = 'UNIQUE'", DB_NAME));
    }

    private String deleteRule(String constraint) {
        return jdbc.queryForObject("SELECT DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS "
                + "WHERE CONSTRAINT_SCHEMA = ? AND CONSTRAINT_NAME = ?", String.class, DB_NAME, constraint);
    }

    @Test
    @DisplayName("DB-01b M1 schema: progress column and legacy curriculum gone, tags/history tables and new columns present")
    void m1SchemaShape() {
        assertEquals(0, count("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_NAME = 'kanban_items' AND COLUMN_NAME IN ('progress', 'tag')", DB_NAME));
        assertEquals(0, count("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_NAME IN ('modules', 'lessons')", DB_NAME));
        assertEquals(2, count("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? "
                + "AND TABLE_NAME IN ('tags', 'card_movements')", DB_NAME));
        assertEquals("enum('IDEATION','IN_PRODUCTION')", columnType("kanban_items", "state"));
        assertEquals("NO", nullable("kanban_items", "team_id"));
        assertEquals("NO", nullable("kanban_items", "uuid"));
        assertEquals("char(36)", columnType("kanban_items", "uuid"));
        for (String table : List.of("kanban_items", "scripts", "courses", "course_modules", "course_lessons")) {
            assertEquals("NO", nullable(table, "uuid"), table + ".uuid must be NOT NULL");
            assertEquals(1, count("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = ? "
                    + "AND TABLE_NAME = ? AND COLUMN_NAME = 'uuid' AND NON_UNIQUE = 0", DB_NAME, table),
                    table + ".uuid must have a unique index");
        }
        assertEquals("YES", nullable("card_tasks", "assignee_member_id"));
        assertEquals("date", columnType("card_tasks", "due_date"));
    }

    @Test
    @DisplayName("DB-02 Foreign keys: deleting a card cascades tasks, script, versions and history; tag/column/assignee set null")
    void foreignKeyBehaviour() {
        Team team = newTeam();
        long cardId = projectId(team, "FK card");
        long taskId = taskId(team, cardId, "t");
        api.patch(team.path() + "/cards/" + cardId + "/tasks/" + taskId, ana.token(),
                map("assigneeMemberId", team.creatorMemberId()));
        api.put("/api/v1/kanban/" + cardId + "/script", ana.token(), map("content", "<p>texto</p>"));
        api.post("/api/v1/kanban/" + cardId + "/script/versions", ana.token(), null);

        long tagId = team.tagIds().get(0);
        jdbc.update("UPDATE kanban_items SET tag_id = ? WHERE id = ?", tagId, cardId);
        assertEquals(1, count("SELECT COUNT(*) FROM card_movements WHERE kanban_item_id = ?", cardId));

        // deleting the tag keeps the card, without a tag
        assertEquals(204, api.delete(team.path() + "/tags/" + tagId, ana.token()).status());
        assertNull(jdbc.queryForObject("SELECT tag_id FROM kanban_items WHERE id = ?", Long.class, cardId));

        // removing the member from the team keeps the task, without assignee
        assertEquals(204, api.delete(team.path() + "/members/" + team.creatorMemberId(), ana.token()).status());
        assertNull(jdbc.queryForObject("SELECT assignee_member_id FROM card_tasks WHERE id = ?", Long.class, taskId));

        // deleting the column keeps the history row, with the column name snapshot and no column id
        long columnId = team.columnIds().get(0);
        jdbc.update("DELETE FROM board_columns WHERE id = ?", columnId);
        Map<String, Object> movement = jdbc.queryForMap(
                "SELECT column_id, column_name FROM card_movements WHERE kanban_item_id = ?", cardId);
        assertNull(movement.get("column_id"));
        assertEquals("Pesquisa", movement.get("column_name"));

        // deleting the card cascades to tasks, script, versions and movements
        assertEquals(204, api.delete(team.path() + "/cards/" + cardId, ana.token()).status());
        assertEquals(0, count("SELECT COUNT(*) FROM card_tasks WHERE kanban_item_id = ?", cardId));
        assertEquals(0, count("SELECT COUNT(*) FROM scripts WHERE kanban_item_id = ?", cardId));
        assertEquals(0, count("SELECT COUNT(*) FROM card_movements WHERE kanban_item_id = ?", cardId));
        assertEquals(0, count("SELECT COUNT(*) FROM script_versions sv WHERE NOT EXISTS "
                + "(SELECT 1 FROM scripts s WHERE s.id = sv.script_id)"));
    }

    @Test
    @DisplayName("DB-02b Deleting a team cascades columns, tags, cards, tasks and members")
    void teamDeleteCascades() {
        Team team = newTeam();
        long cardId = projectId(team, "cascade");
        taskId(team, cardId, "t");
        jdbc.update("DELETE FROM teams WHERE id = ?", team.id());
        assertEquals(0, count("SELECT COUNT(*) FROM board_columns WHERE team_id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM tags WHERE team_id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM kanban_items WHERE team_id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM card_tasks WHERE kanban_item_id = ?", cardId));
        assertEquals(0, count("SELECT COUNT(*) FROM team_members WHERE team_id = ?", team.id()));
    }

    @Test
    @DisplayName("DB-03 Unique constraints: user e-mail, tag name per team (accent/case-insensitive), card uuid")
    void uniqueConstraints() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO users (email, password, name) VALUES (?, 'x', 'Dup')", ana.email()));

        Team team = newTeam();
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO tags (team_id, name, color) VALUES (?, 'tutorial', '#000000')", team.id()),
                "tag name is unique per team, case-insensitively (collation utf8mb4_unicode_ci)");
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO tags (team_id, name, color) VALUES (?, 'Artígo', '#000000')", team.id()),
                "the collation is accent-insensitive: Artigo/Artígo collide");

        long cardId = projectId(team, "uuid");
        String uuid = jdbc.queryForObject("SELECT uuid FROM kanban_items WHERE id = ?", String.class, cardId);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO kanban_items (uuid, title, state, user_id, team_id) VALUES (?, 'dup', 'IDEATION', ?, ?)",
                uuid, ana.id(), team.id()));
    }

    @Test
    @DisplayName("DB-04 Indexes on frequent lookups (card owner, card tasks, history, team)")
    void indexes() {
        Set<String> indexed = Set.copyOf(jdbc.queryForList(
                "SELECT CONCAT(TABLE_NAME, '.', COLUMN_NAME) FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = ? AND SEQ_IN_INDEX = 1", String.class, DB_NAME));
        for (String expected : List.of("kanban_items.user_id", "kanban_items.team_id", "card_tasks.kanban_item_id",
                "card_movements.kanban_item_id", "board_columns.team_id", "script_versions.script_id",
                "team_members.user_id", "tags.team_id")) {
            assertTrue(indexed.contains(expected), "missing index leading with " + expected);
        }
    }

    @Test
    @DisplayName("DB-07 Every card and script gets a distinct uuid (RN05)")
    void uuidsAreDistinct() {
        Team team = newTeam();
        long a = projectId(team, "uuid 1");
        long b = ideaId(team, "uuid 2");
        String ua = jdbc.queryForObject("SELECT uuid FROM kanban_items WHERE id = ?", String.class, a);
        String ub = jdbc.queryForObject("SELECT uuid FROM kanban_items WHERE id = ?", String.class, b);
        assertNotEquals(ua, ub);
        assertDoesNotThrow(() -> UUID.fromString(ua));
        assertDoesNotThrow(() -> UUID.fromString(ub));
        assertEquals(0, count("SELECT COUNT(*) FROM (SELECT uuid FROM kanban_items GROUP BY uuid HAVING COUNT(*) > 1) d"));
    }
}
