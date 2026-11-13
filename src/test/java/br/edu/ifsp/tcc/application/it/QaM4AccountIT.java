package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * QA M4, hunting pass on the account routes (contract 7.13, SEC-23): password change limits, e-mail change code reuse,
 * foreign codes, attempts limit under parallel guesses and the lack of a rate limit on authenticated routes.
 */
@Import(MailCapture.Config.class)
class QaM4AccountIT extends AbstractIT {

    private String newEmail() {
        return "novo." + UUID.randomUUID().toString().substring(0, 8) + "@creatorworkbench.test";
    }

    private String passwordPath(Person p) {
        return "/api/v1/users/" + p.id() + "/password";
    }

    private String changePath(Person p) {
        return "/api/v1/users/" + p.id() + "/email-change";
    }

    private Resp login(String email, String password) {
        return api.post("/api/auth/login", null, map("email", email, "password", password));
    }

    @Test
    @DisplayName("IT-ACC-08 Password change: empty, blank, null, missing and over-72-byte values are 400 and the password stays")
    void passwordValidationMatrix() {
        Person user = person("pw");
        String path = passwordPath(user);

        Object[][] cases = {
                {map("currentPassword", PASSWORD, "newPassword", ""), "empty new"},
                {map("currentPassword", PASSWORD, "newPassword", "        "), "blank new"},
                {map("currentPassword", PASSWORD, "newPassword", "12345"), "5 chars"},
                {map("currentPassword", PASSWORD, "newPassword", "a".repeat(73)), "73 ascii"},
                {map("currentPassword", PASSWORD, "newPassword", "é".repeat(37)), "74 bytes"},
                {map("currentPassword", PASSWORD, "newPassword", "😀".repeat(19)), "76 bytes"},
                {map("currentPassword", PASSWORD, "newPassword", null), "null new"},
                {map("currentPassword", PASSWORD), "missing new"},
                {map("newPassword", "NovaSenha@456"), "missing current"},
                {map("currentPassword", "", "newPassword", "NovaSenha@456"), "empty current"},
                {map("currentPassword", "   ", "newPassword", "NovaSenha@456"), "blank current"},
                {"{}", "empty object"},
                {"[]", "array"},
                {"not json", "garbage"},
        };
        for (Object[] c : cases) {
            Resp r = api.put(path, user.token(), c[0]);
            assertEquals(400, r.status(), c[1] + ": " + r.body());
            assertNotNull(r.message(), c[1] + " has a message");
        }
        // no body at all
        assertEquals(400, api.send("PUT", path, user.token(), null).status());
        // a current password over 72 bytes (multi-byte) is the same 400 "wrong password", never a 500
        Resp longCurrent = api.put(path, user.token(), map("currentPassword", "é".repeat(40), "newPassword", "NovaSenha@456"));
        assertEquals(400, longCurrent.status(), longCurrent.body());
        assertEquals(200, login(user.email(), PASSWORD).status(), "the password never changed");

        // exactly at the limit: 72 bytes in ASCII and in two-byte characters
        assertEquals(204, api.put(path, user.token(), map("currentPassword", PASSWORD, "newPassword", "a".repeat(72))).status());
        assertEquals(200, login(user.email(), "a".repeat(72)).status());
        assertEquals(204, api.put(path, user.token(), map("currentPassword", "a".repeat(72), "newPassword", "é".repeat(36))).status());
        assertEquals(200, login(user.email(), "é".repeat(36)).status());
        // the same password again and surrounding spaces are plain characters
        assertEquals(204, api.put(path, user.token(), map("currentPassword", "é".repeat(36), "newPassword", "é".repeat(36))).status());
        assertEquals(204, api.put(path, user.token(), map("currentPassword", "é".repeat(36), "newPassword", "  com espacos  ")).status());
        assertEquals(200, login(user.email(), "  com espacos  ").status());
        assertEquals(401, login(user.email(), "com espacos").status());
    }

    @Test
    @DisplayName("IT-ACC-09 A wrong current password is 400, never 401: the session stays valid and the user is not logged out")
    void wrongCurrentIsNever401() {
        Person user = person("pw");
        for (int i = 0; i < 5; i++) {
            Resp r = api.put(passwordPath(user), user.token(), map("currentPassword", "Errada@" + i, "newPassword", "NovaSenha@456"));
            assertEquals(400, r.status(), r.body());
            assertEquals("Senha atual incorreta.", r.message());
            assertEquals(200, api.get("/api/v1/users/" + user.id(), user.token()).status(), "still authenticated");
        }
        assertEquals(200, login(user.email(), PASSWORD).status());
        // not someone else's account, whatever the body
        Person other = person("other");
        assertEquals(403, api.put(passwordPath(other), user.token(), map("currentPassword", PASSWORD, "newPassword", "NovaSenha@456")).status());
        assertEquals(401, api.put(passwordPath(user), null, map("currentPassword", PASSWORD, "newPassword", "NovaSenha@456")).status());
        assertEquals(401, api.put(passwordPath(user), "not.a.jwt", map("currentPassword", PASSWORD, "newPassword", "NovaSenha@456")).status());
    }

    @Test
    @DisplayName("IT-ACC-10 / SEC-23 The code of an e-mail change works once and only for its own user and address")
    void codeIsSingleUseAndPersonal() {
        Person user = person("mail");
        Person other = person("peer");
        String target = newEmail();
        String path = changePath(user);
        assertEquals(200, api.post(path, user.token(), map("newEmail", target)).status());
        String code = MailCapture.emailChangeCode(target);

        // somebody else's code does not work on my account, and mine does not work on theirs
        String otherTarget = newEmail();
        assertEquals(200, api.post(changePath(other), other.token(), map("newEmail", otherTarget)).status());
        String otherCode = MailCapture.emailChangeCode(otherTarget);
        assertNotEquals(code, otherCode);
        assertEquals(400, api.post(path + "/confirm", user.token(), map("token", otherCode)).status());
        assertEquals(400, api.post(changePath(other) + "/confirm", other.token(), map("token", code)).status());
        assertEquals(403, api.post(path + "/confirm", other.token(), map("token", code)).status(), "another user's id");
        assertEquals(403, api.post(path, other.token(), map("newEmail", newEmail())).status());
        assertEquals(401, api.post(path + "/confirm", null, map("token", code)).status());

        Resp done = api.post(path + "/confirm", user.token(), map("token", code));
        assertEquals(200, done.status(), done.body());
        assertEquals(target, done.text("email"));
        // second use of the same code, with the e-mail already changed
        Resp reuse = api.post(path + "/confirm", user.token(), map("token", code));
        assertEquals(400, reuse.status(), reuse.body());
        assertEquals(target, api.get("/api/v1/users/" + user.id(), user.token()).text("email"));
        // the old address no longer logs in, the new one does
        assertEquals(401, login(user.email(), PASSWORD).status());
        assertEquals(200, login(target, PASSWORD).status());
        // confirm without any pending request
        assertEquals(400, api.post(path + "/confirm", user.token(), map("token", "123456")).status());
        // the other user's pending request is untouched
        assertEquals(200, api.post(changePath(other) + "/confirm", other.token(), map("token", otherCode)).status());
    }

    @Test
    @DisplayName("IT-ACC-11 E-mail change request: own address in another case and an address in use in another case are 409; spaces trimmed")
    void requestCaseAndSpaces() {
        Person user = person("mail");
        Person taken = person("held");
        String path = changePath(user);

        assertEquals(409, api.post(path, user.token(), map("newEmail", user.email().toUpperCase())).status(), "own address");
        assertEquals(409, api.post(path, user.token(), map("newEmail", taken.email().toUpperCase())).status(), "address of another user");
        assertEquals(400, api.post(path, user.token(), map("newEmail", "sem-arroba")).status());
        assertEquals(400, api.post(path, user.token(), map("newEmail", "a@creatorworkbench.test\r\nBcc: x@creatorworkbench.test")).status());
        assertEquals(400, api.post(path, user.token(), map("newEmail", "")).status());
        assertEquals(400, api.post(path, user.token(), "{}").status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM email_change_tokens WHERE user_id = ?", Integer.class, user.id()));

        // mixed case target: stored as typed, the login and the invites ignore the case
        String mixed = "Troca." + UUID.randomUUID().toString().substring(0, 8) + "@CreatorWorkbench.test";
        assertEquals(200, api.post(path, user.token(), map("newEmail", mixed)).status());
        String code = MailCapture.emailChangeCode(mixed);
        assertEquals(200, api.post(path + "/confirm", user.token(), map("token", code)).status());
        assertEquals(200, login(mixed.toLowerCase(), PASSWORD).status());
        // a failed confirmation of a request for the very same address by someone else now conflicts
        assertEquals(409, api.post(changePath(taken), taken.token(), map("newEmail", mixed.toLowerCase())).status());
    }

    @Test
    @Disabled("BUG QA4-02 (open): parallel wrong guesses are not capped at 5 (31 of 40 were counted as plain wrong codes); remove this line when fixed")
    @DisplayName("IT-ACC-12 / SEC-23 Parallel wrong guesses cannot go beyond 5 attempts: afterwards even the right code is refused")
    void attemptsLimitUnderParallelGuesses() throws Exception {
        Person user = person("mail");
        String target = newEmail();
        String path = changePath(user);
        assertEquals(200, api.post(path, user.token(), map("newEmail", target)).status());
        String code = MailCapture.emailChangeCode(target);
        String wrong = code.equals("000000") ? "000001" : "000000";

        List<Callable<Resp>> calls = new ArrayList<>();
        for (int i = 0; i < 40; i++) calls.add(() -> api.post(path + "/confirm", user.token(), map("token", wrong)));
        List<Resp> results = Parallel.run(calls);

        long counted = results.stream().filter(r -> "Código inválido.".equals(r.message())).count();
        Resp right = api.post(path + "/confirm", user.token(), map("token", code));
        assertAll(
                () -> assertTrue(results.stream().allMatch(r -> r.status() == 400), results.stream().map(Resp::status).toList().toString()),
                () -> assertTrue(counted <= 4, "at most 4 guesses may be answered as plain wrong codes before the 5th discards the code, got " + counted),
                () -> assertEquals(400, right.status(), "the code was discarded after 5 attempts, but the right code gave: " + right.body()),
                () -> assertEquals(user.email(), api.get("/api/v1/users/" + user.id(), user.token()).text("email")));
    }

    @Test
    @Disabled("BUG QA4-03 (open): PUT /users/{id}/password has no rate limit; remove this line when fixed")
    @DisplayName("IT-ACC-13 / SEC-23 The password route is limited like the auth routes: repeated wrong current passwords end in 429")
    void passwordGuessingIsLimited() {
        Person user = person("pw");
        int limited = 0;
        for (int i = 0; i < 40; i++) {
            Resp r = api.put(passwordPath(user), user.token(), map("currentPassword", "Errada@" + i, "newPassword", "NovaSenha@456"));
            if (r.status() == 429) limited++;
            else assertEquals(400, r.status());
        }
        assertTrue(limited > 0, "40 wrong current passwords in a row were all answered, no 429 and no Retry-After");
    }

    @Test
    @Disabled("BUG QA4-03 (open): POST /users/{id}/email-change has no rate limit (mail to arbitrary addresses); remove this line when fixed")
    @DisplayName("IT-ACC-14 / SEC-23 Requests of an e-mail change are limited: 40 requests in a row for other addresses end in 429")
    void emailChangeRequestsAreLimited() {
        Person user = person("mail");
        int limited = 0;
        for (int i = 0; i < 40; i++) {
            Resp r = api.post(changePath(user), user.token(), map("newEmail", newEmail()));
            if (r.status() == 429) limited++;
            else assertEquals(200, r.status(), r.body());
        }
        assertTrue(limited > 0, "40 e-mails with a code were triggered to arbitrary addresses without any limit");
    }
}
