package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Card tasks: create, PATCH (assignee, dueDate), toggle, order, delete (contract 7.4). */
class TasksIT extends AbstractIT {

    private String taskPath(Team team, long cardId) {
        return team.path() + "/cards/" + cardId + "/tasks";
    }

    private List<Long> ids(Resp r) {
        List<Long> ids = new ArrayList<>();
        r.json().forEach(t -> ids.add(t.path("id").asLong()));
        return ids;
    }

    @Test
    @DisplayName("IT-TASK-01 Create returns the extended task (assignee null, dueDate null, sortOrder increments)")
    void create() {
        Team team = newTeam();
        long card = projectId(team, "Tarefas");
        Resp r = api.post(taskPath(team, card), carlos.token(), map("title", "  Gravar intro  "));
        assertEquals(200, r.status(), r.body());
        assertEquals("Gravar intro", r.text("title"));
        assertEquals(card, r.num("cardId"));
        assertFalse(r.at("completed").asBoolean());
        assertNull(r.text("assignee"));
        assertNull(r.text("dueDate"));
        assertEquals(0, r.num("sortOrder"));
        assertEquals(1, api.post(taskPath(team, card), ana.token(), map("title", "outra")).num("sortOrder"));
        assertEquals(400, api.post(taskPath(team, card), ana.token(), map("title", "  ")).status());
        assertEquals(400, api.post(taskPath(team, card), ana.token(), map()).status());
        assertEquals(403, api.post(taskPath(team, card), rui.token(), map("title", "x")).status());
    }

    @Test
    @DisplayName("IT-TASK-02 PATCH sets title, assignee and due date; 0 removes the assignee; empty string clears the date")
    void patch() {
        Team team = newTeam();
        long card = projectId(team, "Edicao");
        long task = taskId(team, card, "Original");
        String path = taskPath(team, card) + "/" + task;

        Resp all = api.patch(path, ana.token(), map("title", "Novo titulo",
                "assigneeMemberId", team.creatorMemberId(), "dueDate", "2030-05-17"));
        assertEquals(200, all.status(), all.body());
        assertEquals("Novo titulo", all.text("title"));
        assertEquals(team.creatorMemberId(), all.num("assignee.memberId"));
        assertEquals(carlos.id(), all.num("assignee.userId"));
        assertEquals("Carlos Teste", all.text("assignee.name"));
        assertEquals("2030-05-17", all.text("dueDate"));

        Resp titleOnly = api.patch(path, carlos.token(), map("title", "So titulo"));
        assertEquals("So titulo", titleOnly.text("title"));
        assertEquals(team.creatorMemberId(), titleOnly.num("assignee.memberId"), "unsent fields stay");
        assertEquals("2030-05-17", titleOnly.text("dueDate"));

        Resp noBody = api.patch(path, ana.token(), map());
        assertEquals(200, noBody.status());
        assertEquals("So titulo", noBody.text("title"));

        Resp cleared = api.patch(path, ana.token(), map("assigneeMemberId", 0, "dueDate", ""));
        assertEquals(200, cleared.status(), cleared.body());
        assertNull(cleared.text("assignee"));
        assertNull(cleared.text("dueDate"));

        // the reviewer can be the assignee (a member of the team)
        Resp reviewer = api.patch(path, ana.token(), map("assigneeMemberId", team.reviewerMemberId()));
        assertEquals(rui.id(), reviewer.num("assignee.userId"));

        // the board shows the same extended task
        Resp board = api.get(team.path() + "/board", rui.token());
        assertEquals(rui.id(), board.at("columns.0.cards.0.tasks.0.assignee.userId").asLong());
    }

    @Test
    @DisplayName("IT-TASK-03 PATCH validation: non-member/unknown assignee, bad date, blank or long title are 400")
    void patchValidation() {
        Team team = newTeam();
        long card = projectId(team, "Validacao");
        long task = taskId(team, card, "t");
        String path = taskPath(team, card) + "/" + task;

        Resp foreignMember = api.patch(path, ana.token(), map("assigneeMemberId", otherTeam.adminMemberId()));
        assertEquals(400, foreignMember.status(), foreignMember.body());
        assertEquals("O responsável precisa ser um membro da equipe.", foreignMember.message());
        assertEquals(400, api.patch(path, ana.token(), map("assigneeMemberId", 999999999)).status());
        assertEquals(400, api.patch(path, ana.token(), map("assigneeMemberId", -1)).status());

        for (String date : List.of("17/05/2030", "2030-5-7", "amanha", "2030-05-17T10:00", " 2030-05-17", "2030-13-01", "2030-02-30")) {
            Resp r = api.patch(path, ana.token(), map("dueDate", date));
            assertEquals(400, r.status(), "dueDate '" + date + "' -> " + r.body());
        }
        assertEquals(400, api.patch(path, ana.token(), map("title", "   ")).status());
        assertEquals(400, api.patch(path, ana.token(), map("title", "x".repeat(256))).status());
        assertEquals(400, api.patch(path, ana.token(), "{\"assigneeMemberId\":\"abc\"}").status());
        assertEquals(400, api.patch(path, ana.token(), "{not json").status());
        assertNull(api.get(team.path() + "/board", ana.token()).at("columns.0.cards.0.tasks.0.dueDate").textValue(),
                "nothing was saved by the rejected requests");
    }

    @Test
    @DisplayName("SEC-TASK-04 Reviewer cannot PATCH, reorder, create or delete tasks (403) but can toggle; foreign ids are 404")
    void permissions() {
        Team team = newTeam();
        long card = projectId(team, "Permissoes");
        long task = taskId(team, card, "t");
        String path = taskPath(team, card);
        assertEquals(403, api.patch(path + "/" + task, rui.token(), map("title", "x")).status());
        assertEquals(403, api.put(path + "/order", rui.token(), map("taskIds", List.of(task))).status());
        assertEquals(403, api.delete(path + "/" + task, rui.token()).status());
        Resp toggled = api.patch(path + "/" + task + "/toggle", rui.token(), null);
        assertEquals(200, toggled.status());
        assertTrue(toggled.at("completed").asBoolean());
        assertFalse(api.patch(path + "/" + task + "/toggle", ana.token(), null).at("completed").asBoolean());

        assertEquals(404, api.patch(path + "/999999999", ana.token(), map("title", "x")).status());
        assertEquals(404, api.patch(team.path() + "/cards/999999999/tasks/" + task, ana.token(), map("title", "x")).status());
        long otherCard = projectId(team, "Outro card");
        assertEquals(404, api.patch(taskPath(team, otherCard) + "/" + task, ana.token(), map("title", "x")).status(),
                "task of another card of the same team");
        assertEquals(403, api.patch(path + "/" + task, bia.token(), map("title", "x")).status());
        assertEquals(404, api.patch(otherTeam.path() + "/cards/" + card + "/tasks/" + task, bia.token(), map("title", "x")).status());
        assertEquals(401, api.patch(path + "/" + task, null, map("title", "x")).status());
        assertEquals(401, api.put(path + "/order", null, map("taskIds", List.of(task))).status());
    }

    @Test
    @DisplayName("IT-TASK-05 PUT tasks/order returns the new order and persists it (board and later reads agree)")
    void reorder() {
        Team team = newTeam();
        long card = projectId(team, "Ordem");
        long a = taskId(team, card, "A");
        long b = taskId(team, card, "B");
        long c = taskId(team, card, "C");
        String path = taskPath(team, card) + "/order";

        Resp r = api.put(path, carlos.token(), map("taskIds", List.of(c, a, b)));
        assertEquals(200, r.status(), r.body());
        assertEquals(List.of(c, a, b), ids(r));
        assertEquals(0, r.num("0.sortOrder"));
        assertEquals(2, r.num("2.sortOrder"));
        Resp board = api.get(team.path() + "/board", ana.token());
        List<Long> boardOrder = new ArrayList<>();
        board.at("columns.0.cards.0.tasks").forEach(t -> boardOrder.add(t.path("id").asLong()));
        assertEquals(List.of(c, a, b), boardOrder);

        // new task goes to the end
        long d = taskId(team, card, "D");
        assertEquals(List.of(c, a, b, d), ids(api.put(path, ana.token(), map("taskIds", List.of(c, a, b, d)))));
    }

    @Test
    @DisplayName("IT-TASK-06 PUT tasks/order rejects missing, extra, repeated, foreign and null ids with 400")
    void reorderValidation() {
        Team team = newTeam();
        long card = projectId(team, "Ordem invalida");
        long other = projectId(team, "Outro");
        long a = taskId(team, card, "A");
        long b = taskId(team, card, "B");
        long foreign = taskId(team, other, "X");
        String path = taskPath(team, card) + "/order";

        for (List<Long> bad : List.of(List.of(a), List.of(a, b, foreign), List.of(a, a), List.of(a, a, b), List.of(a, foreign),
                List.<Long>of(), List.of(a, b, 999999999L))) {
            Resp r = api.put(path, ana.token(), map("taskIds", bad));
            assertEquals(400, r.status(), "ids " + bad + " -> " + r.body());
            assertEquals("A lista deve conter exatamente as tarefas do card.", r.message());
        }
        assertEquals(400, api.put(path, ana.token(), map()).status());
        assertEquals(400, api.put(path, ana.token(), "{\"taskIds\":[null,1]}").status());
        assertEquals(400, api.put(path, ana.token(), "{\"taskIds\":\"1,2\"}").status());
        assertEquals(404, api.put(taskPath(team, 999999999L) + "/order", ana.token(), map("taskIds", List.of(a))).status());
        // unchanged after all the failures
        Resp tasks = api.get(team.path() + "/board", ana.token());
        assertEquals(a, tasks.at("columns.0.cards.0.tasks.0.id").asLong());
        assertNotNull(Arrays.asList(a, b));
    }

    @Test
    @DisplayName("IT-TASK-07 Delete task: 204; second delete 404; deleting the assignee's team membership keeps the task")
    void delete() {
        Team team = newTeam();
        long card = projectId(team, "Apagar");
        long task = taskId(team, card, "t");
        api.patch(taskPath(team, card) + "/" + task, ana.token(), map("assigneeMemberId", team.creatorMemberId()));
        assertEquals(204, api.delete(team.path() + "/members/" + team.creatorMemberId(), ana.token()).status());
        Resp board = api.get(team.path() + "/board", ana.token());
        assertEquals(1, board.at("columns.0.cards.0.tasks").size());
        assertNull(board.at("columns.0.cards.0.tasks.0.assignee").textValue());
        assertEquals(204, api.delete(taskPath(team, card) + "/" + task, ana.token()).status());
        assertEquals(404, api.delete(taskPath(team, card) + "/" + task, ana.token()).status());
    }
}
