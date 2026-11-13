package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Contract 7.14: rename and delete a team. */
@Import(MailCapture.Config.class)
class TeamAdminIT extends AbstractIT {

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    @DisplayName("IT-TEAM-01 PATCH team: ADMIN renames and gets the team in the list format; others 403")
    void rename() {
        Team team = newTeam();

        Resp renamed = api.patch("/api/v1/teams/" + team.id(), ana.token(), map("name", "  Office Resolve Cursos "));

        assertEquals(200, renamed.status(), renamed.body());
        assertEquals(team.id(), renamed.num("id"));
        assertEquals("Office Resolve Cursos", renamed.text("name"));
        assertEquals("ADMIN", renamed.text("role"));
        assertEquals(3, renamed.num("membersCount"));
        assertTrue(renamed.text("createdAt").endsWith("Z"));
        assertEquals("Office Resolve Cursos", jdbc.queryForObject("SELECT name FROM teams WHERE id = ?", String.class, team.id()));

        assertEquals(403, api.patch("/api/v1/teams/" + team.id(), carlos.token(), map("name", "x")).status());
        assertEquals(403, api.patch("/api/v1/teams/" + team.id(), rui.token(), map("name", "x")).status());
        assertEquals(403, api.patch("/api/v1/teams/" + team.id(), bia.token(), map("name", "x")).status());
        assertEquals(400, api.patch("/api/v1/teams/" + team.id(), ana.token(), map("name", "   ")).status());
        assertEquals(400, api.patch("/api/v1/teams/" + team.id(), ana.token(), map("name", "x".repeat(121))).status());
        assertEquals(401, api.patch("/api/v1/teams/" + team.id(), null, map("name", "x")).status());
    }

    @Test
    @DisplayName("IT-TEAM-02 DELETE team: 204 removes members, columns, tags, cards, tasks and invites; others 403")
    void deleteTeam() {
        Team team = newTeam();
        long card = projectId(team, "Para apagar");
        long task = taskId(team, card, "tarefa");
        long idea = ideaId(team, "Ideia para apagar");
        api.post(team.path() + "/invites", ana.token(), map("email", "x." + UUID.randomUUID() + "@creatorworkbench.test", "role", "CREATOR"));
        assertEquals(1, count("SELECT COUNT(*) FROM team_invites WHERE team_id = ?", team.id()));

        assertEquals(403, api.delete("/api/v1/teams/" + team.id(), carlos.token()).status());
        assertEquals(403, api.delete("/api/v1/teams/" + team.id(), rui.token()).status());
        assertEquals(403, api.delete("/api/v1/teams/" + team.id(), bia.token()).status());
        assertEquals(1, count("SELECT COUNT(*) FROM teams WHERE id = ?", team.id()));

        Resp deleted = api.delete("/api/v1/teams/" + team.id(), ana.token());

        assertEquals(204, deleted.status(), deleted.body());
        assertEquals("", deleted.body());
        assertEquals(0, count("SELECT COUNT(*) FROM teams WHERE id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM team_members WHERE team_id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM board_columns WHERE team_id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM tags WHERE team_id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM kanban_items WHERE team_id = ?", team.id()));
        assertEquals(0, count("SELECT COUNT(*) FROM kanban_items WHERE id IN (?, ?)", card, idea));
        assertEquals(0, count("SELECT COUNT(*) FROM card_tasks WHERE id = ?", task));
        assertEquals(0, count("SELECT COUNT(*) FROM team_invites WHERE team_id = ?", team.id()));
        // the users survive and lose only the team
        assertEquals(1, count("SELECT COUNT(*) FROM users WHERE id = ?", carlos.id()));
        assertEquals(403, api.get(team.path() + "/board", ana.token()).status());
    }

    @Test
    @DisplayName("IT-TEAM-03 The personal team cannot be deleted (409); a shared team of the same user can")
    void personalTeamIsProtected() {
        Person solo = person("solo");
        Resp teams = api.get("/api/v1/teams", solo.token());
        assertEquals(200, teams.status(), teams.body());
        long personalId = teams.at("0.id").asLong();
        assertEquals(1, count("SELECT personal FROM teams WHERE id = ?", personalId));
        assertTrue(teams.at("0.personal").asBoolean());

        Resp refused = api.delete("/api/v1/teams/" + personalId, solo.token());

        assertEquals(409, refused.status());
        assertEquals("A equipe pessoal não pode ser excluída.", refused.message());
        assertEquals(1, count("SELECT COUNT(*) FROM teams WHERE id = ?", personalId));
        // it can still be renamed
        Resp renamedPersonal = api.patch("/api/v1/teams/" + personalId, solo.token(), map("name", "Meu estúdio novo"));
        assertEquals(200, renamedPersonal.status());
        assertTrue(renamedPersonal.at("personal").asBoolean(), "renaming keeps it personal");
        assertEquals(409, api.delete("/api/v1/teams/" + personalId, solo.token()).status());

        Resp created = api.post("/api/v1/teams", solo.token(), map("name", "Equipe comum"));
        assertEquals(0, count("SELECT personal FROM teams WHERE id = ?", created.num("id")));
        assertFalse(created.at("personal").asBoolean());
        assertEquals(204, api.delete("/api/v1/teams/" + created.num("id"), solo.token()).status());
    }
}
