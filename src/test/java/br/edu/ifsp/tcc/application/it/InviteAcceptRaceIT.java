package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.service.InviteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Parallel accepts of invites cannot take more seats than the plan has. */
@Import(MailCapture.Config.class)
class InviteAcceptRaceIT extends AbstractIT {

    @Test
    @DisplayName("IT-INV-19 Two accepts at the same time with one seat left: one member is added, the other gets 402")
    void parallelAcceptsRespectTheMemberLimit() throws Exception {
        Team team = newTeam();                       // Agência: 5 users, 3 members now
        Person extra = person("extra");
        addMemberDirect(team.id(), extra, "CREATOR");
        // 4 members: one seat left. Two valid invites (stored directly, as if sent before the team filled up)
        Person dora = person("dora");
        Person edu = person("edu");
        List<String> tokens = new ArrayList<>();
        for (Person p : List.of(dora, edu)) {
            String token = "race-" + p.id() + "-" + System.nanoTime();
            tokens.add(token);
            jdbc.update("INSERT INTO team_invites (team_id, email, role, token_hash, status, invited_by, expires_at) "
                    + "VALUES (?, ?, 'CREATOR', ?, 'PENDING', ?, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 DAY))",
                    team.id(), p.email(), InviteService.hash(token), ana.id());
        }

        List<Callable<Resp>> calls = List.of(
                () -> api.post("/api/v1/invites/" + tokens.get(0) + "/accept", dora.token(), null),
                () -> api.post("/api/v1/invites/" + tokens.get(1) + "/accept", edu.token(), null));
        List<Resp> results = Parallel.run(calls);

        assertEquals(1, results.stream().filter(r -> r.status() == 200).count(), results.stream().map(r -> r.status() + " " + r.body()).toList().toString());
        assertEquals(1, results.stream().filter(r -> r.status() == 402).count());
        assertEquals(5, jdbc.queryForObject("SELECT COUNT(*) FROM team_members WHERE team_id = ?", Integer.class, team.id()));
    }
}
