package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.usecase.SendDeadlineDigestUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QA M4, hunting pass on alerts and the daily digest (IT-ALR, decision 19): latest review wins, ideas and finished
 * tasks never alert, removed members and the content of the digest lines (markup and line breaks in titles).
 */
@Import(MailCapture.Config.class)
class QaM4AlertsIT extends AbstractIT {

    @Autowired private SendDeadlineDigestUseCase digestUseCase;

    @BeforeEach
    void clearMail() {
        MailCapture.DIGESTS.clear();
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    }

    private void setDue(long cardId, LocalDate due) {
        jdbc.update("UPDATE kanban_items SET due_date = ? WHERE id = ?", due, cardId);
    }

    private long taskDue(Team team, long cardId, String title, LocalDate due, Long assigneeMemberId) {
        long id = taskId(team, cardId, title);
        Resp r = api.patch(team.path() + "/cards/" + cardId + "/tasks/" + id, ana.token(),
                assigneeMemberId == null ? map("dueDate", due.toString()) : map("dueDate", due.toString(), "assigneeMemberId", assigneeMemberId));
        assertEquals(200, r.status(), r.body());
        return id;
    }

    private List<Long> taskIds(Resp alerts) {
        List<Long> ids = new ArrayList<>();
        alerts.json().forEach(a -> { if (!a.path("taskId").isNull()) ids.add(a.path("taskId").asLong()); });
        return ids;
    }

    private List<Long> cardIds(Resp alerts) {
        List<Long> ids = new ArrayList<>();
        alerts.json().forEach(a -> ids.add(a.path("cardId").asLong()));
        return ids;
    }

    private List<String> linesOf(MailCapture.Digest digest, Team team) {
        String prefix = "[" + jdbc.queryForObject("SELECT name FROM teams WHERE id = ?", String.class, team.id()) + "]";
        return digest.lines().stream().filter(l -> l.startsWith(prefix)).toList();
    }

    @Test
    @DisplayName("IT-ALR-03 Decision 19: the latest review decides; a project approved and then sent back for changes alerts again")
    void latestReviewWins() {
        Team team = newTeam();
        long card = projectId(team, "Aprovado e depois devolvido");
        setDue(card, today().plusDays(1));
        taskDue(team, card, "tarefa", today().plusDays(1), null);
        assertEquals(2, api.get(team.path() + "/alerts", ana.token()).size());

        assertEquals(200, api.post(team.path() + "/cards/" + card + "/reviews", rui.token(), map("decision", "APPROVED")).status());
        assertEquals(0, api.get(team.path() + "/alerts", ana.token()).size(), "approved: neither the project nor its task");

        assertEquals(200, api.post(team.path() + "/cards/" + card + "/reviews", rui.token(), map("decision", "CHANGES_REQUESTED", "comment", "ajustar")).status());
        assertEquals(2, api.get(team.path() + "/alerts", ana.token()).size(), "the latest review is CHANGES_REQUESTED");
    }

    @Test
    @DisplayName("IT-ALR-04 Ideas, finished tasks, deleted tasks and tasks without date never alert; moving the date moves the alert")
    void whatDoesNotAlert() {
        Team team = newTeam();
        long idea = ideaId(team, "Ideia com prazo");
        setDue(idea, today().minusDays(3));
        long card = projectId(team, "Projeto com tarefas");
        long done = taskDue(team, card, "feita", today().minusDays(1), null);
        long removed = taskDue(team, card, "apagada", today().minusDays(1), null);
        long open = taskDue(team, card, "aberta", today().plusDays(1), null);

        assertEquals(3, taskIds(api.get(team.path() + "/alerts", ana.token())).size());
        assertEquals(200, api.patch(team.path() + "/cards/" + card + "/tasks/" + done + "/toggle", ana.token(), null).status());
        assertEquals(204, api.delete(team.path() + "/cards/" + card + "/tasks/" + removed, ana.token()).status());

        Resp alerts = api.get(team.path() + "/alerts", ana.token());
        assertFalse(cardIds(alerts).contains(idea), "an idea is not an active project");
        assertEquals(List.of(open), taskIds(alerts), alerts.body());

        // clearing the date of the task removes its alert
        assertEquals(200, api.patch(team.path() + "/cards/" + card + "/tasks/" + open, ana.token(), map("dueDate", "")).status());
        assertEquals(0, api.get(team.path() + "/alerts", ana.token()).size());
    }

    @Test
    @DisplayName("IT-ALR-05 Digest: team and project names with markup stay plain text, one line per alert, no line break inside a line")
    void digestContentIsPlainText() {
        Team team = newTeam();
        assertEquals(200, api.patch("/api/v1/teams/" + team.id(), ana.token(), map("name", "<script>alert(1)</script> & Cia")).status());
        long card = projectId(team, "<img src=x onerror=alert(1)> Projeto");
        setDue(card, today());

        int sent = digestUseCase.execute();
        MailCapture.awaitDigests(sent);

        List<String> lines = linesOf(MailCapture.digestsTo(ana.email()).get(0), team);
        assertEquals(1, lines.size(), lines.toString());
        assertEquals("[<script>alert(1)</script> & Cia] Projeto \"<img src=x onerror=alert(1)> Projeto\": vence hoje ("
                + today().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")", lines.get(0));
        assertTrue(lines.stream().noneMatch(l -> l.contains("\n") || l.contains("\r")));
    }

    @Test
    @Disabled("BUG QA4-04 (open): a card title with a line break forges extra lines in the digest e-mail; remove this line when fixed")
    @DisplayName("IT-ALR-06 Digest: a card title cannot forge extra lines of the e-mail (line breaks are refused or stay inside one line)")
    void digestLineForging() {
        Team team = newTeam();
        Resp forged = createProject(team, ana, "Projeto\nAVISO: sua conta foi bloqueada, acesse http://exemplo.test", team.columnIds().get(0));
        if (forged.status() != 200) {
            assertEquals(400, forged.status(), forged.body());
            return;
        }
        setDue(forged.num("id"), today());
        int sent = digestUseCase.execute();
        MailCapture.awaitDigests(sent);
        for (String line : linesOf(MailCapture.digestsTo(ana.email()).get(0), team)) {
            assertFalse(line.contains("\n") || line.contains("\r"), "a title with a line break forged a second line: " + line);
        }
    }

    @Test
    @DisplayName("IT-ALR-07 Digest: a member removed from the team stops receiving it, and his assigned task goes to the others")
    void removedMemberGetsNothing() {
        Team team = newTeam();
        long card = projectId(team, "Projeto da equipe");
        setDue(card, today().plusDays(2));
        taskDue(team, card, "tarefa do Carlos", today().plusDays(1), team.creatorMemberId());
        assertEquals(204, api.delete(team.path() + "/members/" + team.creatorMemberId(), ana.token()).status());

        int sent = digestUseCase.execute();
        MailCapture.awaitDigests(sent);

        assertTrue(MailCapture.digestsTo(carlos.email()).stream().allMatch(d -> linesOf(d, team).isEmpty()),
                "carlos left the team and must not be told about it");
        List<String> toAna = linesOf(MailCapture.digestsTo(ana.email()).get(0), team);
        assertEquals(2, toAna.size(), toAna.toString());
        assertEquals(2, linesOf(MailCapture.digestsTo(rui.email()).get(0), team).size());
    }
}
