package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.usecase.SendDeadlineDigestUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Contract 7.12: deadline alerts and the daily digest (RF09). */
@Import(MailCapture.Config.class)
class AlertsIT extends AbstractIT {

    @Autowired private SendDeadlineDigestUseCase digestUseCase;

    @BeforeEach
    void clearMail() {
        MailCapture.DIGESTS.clear();
    }

    /** The date the users see: Sao Paulo, not UTC. */
    private LocalDate today() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    }

    private void setCardDue(long cardId, LocalDate due) {
        jdbc.update("UPDATE kanban_items SET due_date = ? WHERE id = ?", due, cardId);
    }

    private long taskDue(Team team, long cardId, String title, LocalDate due, Long assigneeMemberId) {
        long id = taskId(team, cardId, title);
        Resp r = api.patch(team.path() + "/cards/" + cardId + "/tasks/" + id, ana.token(),
                assigneeMemberId == null ? map("dueDate", due.toString())
                        : map("dueDate", due.toString(), "assigneeMemberId", assigneeMemberId));
        assertEquals(200, r.status(), r.body());
        return id;
    }

    @Test
    @DisplayName("IT-ALERT-01 GET alerts: classifies cards and tasks, 3-day window, closest deadline first, shape of the contract")
    void alerts() {
        Team team = newTeam();
        long overdueCard = projectId(team, "Webinar atrasado");
        long soonCard = projectId(team, "Aula em breve");
        long farCard = projectId(team, "Aula distante");
        long noDateCard = projectId(team, "Sem prazo");
        setCardDue(overdueCard, today().minusDays(1));
        setCardDue(soonCard, today().plusDays(3));
        setCardDue(farCard, today().plusDays(4));
        long overdueTask = taskDue(team, noDateCard, "Gravar", today().minusDays(5), null);
        long soonTask = taskDue(team, noDateCard, "Revisar o roteiro", today(), team.creatorMemberId());
        long farTask = taskDue(team, noDateCard, "Publicar", today().plusDays(30), null);
        long doneTask = taskDue(team, noDateCard, "Já feita", today().minusDays(2), null);
        assertEquals(200, api.patch(team.path() + "/cards/" + noDateCard + "/tasks/" + doneTask + "/toggle", ana.token(), null).status());
        long idea = ideaId(team, "Ideia com prazo");
        setCardDue(idea, today().minusDays(10));

        Resp alerts = api.get(team.path() + "/alerts", rui.token());

        assertEquals(200, alerts.status(), alerts.body());
        assertEquals(4, alerts.size(), alerts.body());
        assertEquals("TASK_OVERDUE", alerts.at("0.type").asText());
        assertEquals(noDateCard, alerts.at("0.cardId").asLong());
        assertEquals("Sem prazo", alerts.at("0.cardTitle").asText());
        assertEquals(overdueTask, alerts.at("0.taskId").asLong());
        assertEquals("Gravar", alerts.at("0.taskTitle").asText());
        assertEquals(today().minusDays(5).toString(), alerts.at("0.dueDate").asText());
        assertEquals(-5, alerts.at("0.daysLeft").asInt());

        assertEquals("CARD_OVERDUE", alerts.at("1.type").asText());
        assertEquals(overdueCard, alerts.at("1.cardId").asLong());
        assertTrue(alerts.at("1.taskId").isNull());
        assertTrue(alerts.at("1.taskTitle").isNull());
        assertEquals(-1, alerts.at("1.daysLeft").asInt());

        assertEquals("TASK_DUE_SOON", alerts.at("2.type").asText());
        assertEquals(soonTask, alerts.at("2.taskId").asLong());
        assertEquals(0, alerts.at("2.daysLeft").asInt());

        assertEquals("CARD_DUE_SOON", alerts.at("3.type").asText());
        assertEquals(soonCard, alerts.at("3.cardId").asLong());
        assertEquals(3, alerts.at("3.daysLeft").asInt());
        assertEquals(7, alerts.at("0").size(), "type, cardId, cardTitle, taskId, taskTitle, dueDate, daysLeft");
        assertNotEquals(farTask, alerts.at("2.taskId").asLong());
        assertNotEquals(farCard, alerts.at("3.cardId").asLong());
    }

    @Test
    @DisplayName("IT-ALERT-02 Every member may read alerts (Admin, Criador, Revisor); outsiders 403; no token 401; empty list 200")
    void roles() {
        Team team = newTeam();
        assertEquals("[]", api.get(team.path() + "/alerts", ana.token()).body());
        for (Person who : new Person[] {ana, carlos, rui}) {
            assertEquals(200, api.get(team.path() + "/alerts", who.token()).status());
        }
        assertEquals(403, api.get(team.path() + "/alerts", bia.token()).status());
        assertEquals(403, api.get(team.path() + "/alerts", loner.token()).status());
        assertEquals(401, api.get(team.path() + "/alerts", null).status());
    }

    @Test
    @DisplayName("IT-ALERT-03 The date is the Sao Paulo date: a card due today is DUE_SOON with daysLeft 0, never overdue")
    void usesSaoPauloDate() {
        Team team = newTeam();
        long card = projectId(team, "Vence hoje");
        setCardDue(card, today());

        Resp alerts = api.get(team.path() + "/alerts", ana.token());

        assertEquals(1, alerts.size());
        assertEquals("CARD_DUE_SOON", alerts.at("0.type").asText());
        assertEquals(0, alerts.at("0.daysLeft").asInt());
    }

    @Test
    @DisplayName("IT-ALERT-04 Daily digest: one e-mail per user across all teams; tasks go to the assignee or to everyone when unassigned")
    void digest() {
        Team teamA = newTeam();
        long cardA = projectId(teamA, "Projeto A");
        setCardDue(cardA, today().minusDays(2));
        taskDue(teamA, cardA, "Tarefa do Carlos", today().plusDays(1), teamA.creatorMemberId());
        taskDue(teamA, cardA, "Tarefa sem dono", today().plusDays(2), null);

        Team teamB = newTeam();
        long cardB = projectId(teamB, "Projeto B");
        setCardDue(cardB, today().plusDays(1));
        Team quiet = newTeam();
        projectId(quiet, "Sem prazos");

        int sent = digestUseCase.execute();
        MailCapture.awaitDigests(sent);

        assertTrue(sent >= 3);
        // Ana: admin of A, B and the quiet team: one e-mail with the lines of A and B
        List<MailCapture.Digest> toAna = MailCapture.digestsTo(ana.email());
        assertEquals(1, toAna.size());
        List<String> lines = mine(toAna.get(0), teamA, teamB, quiet);
        assertEquals(3, lines.size(), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.contains("Projeto \"Projeto A\": venceu há 2 dias")), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.contains("Tarefa \"Tarefa sem dono\" (projeto \"Projeto A\"): vence em 2 dias")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Projeto \"Projeto B\": vence amanhã")));
        assertTrue(lines.stream().noneMatch(l -> l.contains("Tarefa do Carlos")), "assigned to Carlos only");
        assertTrue(lines.stream().noneMatch(l -> l.contains("Sem prazos")));
        assertEquals("Ana Teste", toAna.get(0).name());

        // Carlos (creator in A and B): the card alerts, his own task and the unassigned one
        List<MailCapture.Digest> toCarlos = MailCapture.digestsTo(carlos.email());
        assertEquals(1, toCarlos.size());
        assertEquals(4, mine(toCarlos.get(0), teamA, teamB, quiet).size());
        assertTrue(toCarlos.get(0).lines().stream().anyMatch(l -> l.contains("Tarefa do Carlos")));

        // Rui (reviewer) also receives the team alerts, except the task assigned to someone else
        assertEquals(3, mine(MailCapture.digestsTo(rui.email()).get(0), teamA, teamB, quiet).size());
        // people outside these teams get nothing
        assertTrue(MailCapture.digestsTo(bia.email()).isEmpty());
        assertTrue(MailCapture.digestsTo(loner.email()).isEmpty());
    }

    @Test
    @DisplayName("IT-ALERT-05 Decision 19: approved projects and their tasks produce no alerts; changes requested or no review still do")
    void onlyActiveProjects() {
        Team team = newTeam();
        long approved = projectId(team, "Aprovado");
        long changes = projectId(team, "Com ajustes");
        long unreviewed = projectId(team, "Sem revisao");
        for (long c : new long[] {approved, changes, unreviewed}) setCardDue(c, today().minusDays(2));
        taskDue(team, approved, "tarefa do aprovado", today().minusDays(1), null);
        taskDue(team, changes, "tarefa do com ajustes", today().minusDays(1), null);
        assertEquals(200, api.post(team.path() + "/cards/" + approved + "/reviews", rui.token(), map("decision", "APPROVED")).status());
        assertEquals(200, api.post(team.path() + "/cards/" + changes + "/reviews", rui.token(), map("decision", "CHANGES_REQUESTED", "comment", "ajustar")).status());

        Resp alerts = api.get(team.path() + "/alerts", carlos.token());

        assertEquals(3, alerts.size(), alerts.body());
        List<Long> cardIds = new java.util.ArrayList<>();
        alerts.json().forEach(a -> cardIds.add(a.path("cardId").asLong()));
        assertFalse(cardIds.contains(approved));
        assertTrue(cardIds.contains(changes) && cardIds.contains(unreviewed));

        // a new approval after the changes takes the project out too
        assertEquals(200, api.post(team.path() + "/cards/" + changes + "/reviews", rui.token(), map("decision", "APPROVED")).status());
        assertEquals(1, api.get(team.path() + "/alerts", carlos.token()).size());

        // the digest follows the same rule
        MailCapture.DIGESTS.clear();
        int sent = digestUseCase.execute();
        MailCapture.awaitDigests(sent);
        List<String> lines = mine(MailCapture.digestsTo(ana.email()).get(0), team);
        assertEquals(1, lines.size(), lines.toString());
        assertTrue(lines.get(0).contains("Sem revisao"));
    }

    /** The lines of a digest that belong to the given teams (the same people take part in other tests). */
    private List<String> mine(MailCapture.Digest digest, Team... teams) {
        List<String> prefixes = java.util.Arrays.stream(teams).map(t -> "["
                + jdbc.queryForObject("SELECT name FROM teams WHERE id = ?", String.class, t.id()) + "]").toList();
        return digest.lines().stream().filter(l -> prefixes.stream().anyMatch(l::startsWith)).toList();
    }
}
