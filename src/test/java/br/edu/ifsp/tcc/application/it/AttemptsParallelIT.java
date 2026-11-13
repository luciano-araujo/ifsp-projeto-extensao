package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** QA4-02: the attempt limit of the sign-up and password-reset codes holds under parallel guesses (as the e-mail change). */
class AttemptsParallelIT extends AbstractIT {

    // Fewer than the per-route limit of the auth rate limiter (12 in the IT profile), so every guess reaches the use case
    private static final int GUESSES = 10;

    @Test
    @DisplayName("IT-ATT-01 Sign-up code: parallel wrong guesses cannot go beyond 5 attempts; afterwards the right code is refused")
    void registrationCode() throws Exception {
        String email = "reg." + UUID.randomUUID().toString().substring(0, 8) + "@creatorworkbench.test";
        jdbc.update("INSERT INTO registration_tokens (email, name, token, attempts, expires_at) "
                + "VALUES (?, 'Nova Pessoa', '482917', 0, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE))", email);

        List<Callable<Resp>> calls = new ArrayList<>();
        for (int i = 0; i < GUESSES; i++) {
            calls.add(() -> api.post("/api/auth/register/confirm", null,
                    map("email", email, "token", "000000", "password", "Senha@123")));
        }
        List<Resp> results = Parallel.run(calls);

        assertTrue(results.stream().allMatch(r -> r.status() == 400), results.stream().map(Resp::status).toList().toString());
        Resp right = api.post("/api/auth/register/confirm", null, map("email", email, "token", "482917", "password", "Senha@123"));
        assertEquals(400, right.status(), "the code was discarded after 5 attempts, but the right code gave: " + right.body());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email));
    }

    @Test
    @DisplayName("IT-ATT-02 Password-reset code: parallel wrong guesses cannot go beyond 5 attempts; afterwards the right code is refused")
    void resetCode() throws Exception {
        Person user = person("reset");
        jdbc.update("INSERT INTO password_reset_tokens (email, token, attempts, expires_at) "
                + "VALUES (?, '482917', 0, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE))", user.email());

        List<Callable<Resp>> calls = new ArrayList<>();
        for (int i = 0; i < GUESSES; i++) {
            calls.add(() -> api.post("/api/auth/password/reset", null,
                    map("email", user.email(), "token", "000000", "newPassword", "OutraSenha@456")));
        }
        List<Resp> results = Parallel.run(calls);

        assertTrue(results.stream().allMatch(r -> r.status() == 400), results.stream().map(Resp::status).toList().toString());
        Resp right = api.post("/api/auth/password/reset", null,
                map("email", user.email(), "token", "482917", "newPassword", "OutraSenha@456"));
        assertEquals(400, right.status(), "the code was discarded after 5 attempts, but the right code gave: " + right.body());
        assertEquals(200, api.post("/api/auth/login", null, map("email", user.email(), "password", PASSWORD)).status(),
                "the password did not change");
    }

    /** 8 wrong guesses and the right code at the same moment; the right one may only win while the cap is not reached. */
    private List<Resp> burstWithTheRightCode(java.util.function.Function<String, Resp> confirm, String right) throws Exception {
        List<Callable<Resp>> calls = new ArrayList<>();
        for (int i = 0; i < 4; i++) calls.add(() -> confirm.apply("000000"));
        calls.add(() -> confirm.apply(right));
        for (int i = 0; i < 4; i++) calls.add(() -> confirm.apply("000001"));
        return Parallel.run(calls);
    }

    @Test
    @DisplayName("IT-ATT-03 Sign-up code: a burst of wrong guesses plus the right code is capped at 5 attempts in total, and the code is gone afterwards")
    void registrationBurstWithTheRightCode() throws Exception {
        String email = "reg." + UUID.randomUUID().toString().substring(0, 8) + "@creatorworkbench.test";
        jdbc.update("INSERT INTO registration_tokens (email, name, token, attempts, expires_at) "
                + "VALUES (?, 'Nova Pessoa', '482917', 0, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE))", email);

        List<Resp> results = burstWithTheRightCode(code -> api.post("/api/auth/register/confirm", null,
                map("email", email, "token", code, "password", "Senha@123")), "482917");

        assertTrue(results.stream().filter(r -> r.status() == 200).count() <= 1, results.stream().map(Resp::status).toList().toString());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM registration_tokens WHERE email = ?", Integer.class, email),
                "five attempts were spent: the code is gone (or was used)");
        assertEquals(400, api.post("/api/auth/register/confirm", null,
                map("email", email, "token", "482917", "password", "Senha@123")).status());
    }

    @Test
    @DisplayName("IT-ATT-04 Reset code: a burst of wrong guesses plus the right code is capped at 5 attempts in total, and the code is gone afterwards")
    void resetBurstWithTheRightCode() throws Exception {
        Person user = person("reset");
        jdbc.update("INSERT INTO password_reset_tokens (email, token, attempts, expires_at) "
                + "VALUES (?, '482917', 0, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE))", user.email());

        List<Resp> results = burstWithTheRightCode(code -> api.post("/api/auth/password/reset", null,
                map("email", user.email(), "token", code, "newPassword", "OutraSenha@456")), "482917");

        assertTrue(results.stream().filter(r -> r.status() == 200).count() <= 1);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE email = ?", Integer.class, user.email()));
        assertEquals(400, api.post("/api/auth/password/reset", null,
                map("email", user.email(), "token", "482917", "newPassword", "Terceira@789")).status());
    }

    @Test
    @DisplayName("IT-ATT-05 E-mail change code: a burst of wrong guesses plus the right code is capped at 5 attempts in total, and the code is gone afterwards")
    void emailChangeBurstWithTheRightCode() throws Exception {
        Person user = person("mail");
        String target = "novo." + UUID.randomUUID().toString().substring(0, 8) + "@creatorworkbench.test";
        jdbc.update("INSERT INTO email_change_tokens (user_id, new_email, token, attempts, expires_at) "
                + "VALUES (?, ?, '482917', 0, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE))", user.id(), target);
        String path = "/api/v1/users/" + user.id() + "/email-change/confirm";

        List<Resp> results = burstWithTheRightCode(code -> api.post(path, user.token(), map("token", code)), "482917");

        assertTrue(results.stream().filter(r -> r.status() == 200).count() <= 1);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM email_change_tokens WHERE user_id = ?", Integer.class, user.id()));
        assertEquals(400, api.post(path, user.token(), map("token", "482917")).status());
    }
}
