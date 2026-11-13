package br.edu.ifsp.tcc.application.it;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Contract 7.13: change password and change e-mail. */
@Import(MailCapture.Config.class)
class AccountIT extends AbstractIT {

    private String newEmail() {
        return "novo." + UUID.randomUUID().toString().substring(0, 8) + "@creatorworkbench.test";
    }

    private Resp login(String email, String password) {
        return api.post("/api/auth/login", null, map("email", email, "password", password));
    }

    @Test
    @DisplayName("IT-ACC-00 PUT /users/{id} changes only the name: e-mail and password in the body are ignored")
    void updateUserIgnoresEmailAndPassword() {
        Person user = person("upd");

        Resp updated = api.put("/api/v1/users/" + user.id(), user.token(),
                map("name", "Nome Novo", "email", newEmail(), "password", "OutraSenha@789"));

        assertEquals(200, updated.status(), updated.body());
        assertEquals("Nome Novo", updated.text("name"));
        assertEquals(user.email(), updated.text("email"));
        assertEquals(user.email(), api.get("/api/v1/users/" + user.id(), user.token()).text("email"));
        assertEquals(200, login(user.email(), PASSWORD).status());
        assertEquals(401, login(user.email(), "OutraSenha@789").status());
    }

    @Test
    @DisplayName("IT-ACC-01 PUT password: 200 {token}, the new password logs in, the old one does not, the old token dies and the new one works")
    void changePassword() {
        Person user = person("pwd");

        Resp changed = api.put("/api/v1/users/" + user.id() + "/password", user.token(),
                map("currentPassword", PASSWORD, "newPassword", "NovaSenha@456"));

        assertEquals(200, changed.status(), changed.body());
        String fresh = changed.text("token");
        assertNotNull(fresh);
        assertEquals(200, login(user.email(), "NovaSenha@456").status());
        assertEquals(401, login(user.email(), PASSWORD).status());
        // the other sessions are gone (token version + 1); the token of the answer is the current session
        assertEquals(401, api.get("/api/v1/users/" + user.id(), user.token()).status());
        assertEquals(200, api.get("/api/v1/users/" + user.id(), fresh).status());
        assertEquals(1, jdbc.queryForObject("SELECT token_version FROM users WHERE id = ?", Integer.class, user.id()));
        // a login after the change gets a working token too
        assertEquals(200, api.get("/api/v1/users/" + user.id(), login(user.email(), "NovaSenha@456").text("token")).status());
    }

    @Test
    @DisplayName("IT-ACC-02 PUT password errors: wrong current 400, rules of registration (6..72 chars, 72 bytes) 400, other user 403")
    void changePasswordErrors() {
        Person user = person("pwd");
        String path = "/api/v1/users/" + user.id() + "/password";

        Resp wrong = api.put(path, user.token(), map("currentPassword", "errada", "newPassword", "NovaSenha@456"));
        assertEquals(400, wrong.status());
        assertEquals("Senha atual incorreta.", wrong.message());

        assertEquals(400, api.put(path, user.token(), map("currentPassword", PASSWORD, "newPassword", "12345")).status());
        assertEquals(400, api.put(path, user.token(), map("currentPassword", PASSWORD, "newPassword", "a".repeat(73))).status());
        // 37 accented characters: under 72 characters, over 72 bytes
        assertEquals(400, api.put(path, user.token(), map("currentPassword", PASSWORD, "newPassword", "é".repeat(37))).status());
        assertEquals(400, api.put(path, user.token(), map("currentPassword", "", "newPassword", "NovaSenha@456")).status());
        assertEquals(400, api.put(path, user.token(), map("currentPassword", PASSWORD)).status());
        // a 72-byte password is accepted
        assertEquals(200, api.put(path, user.token(), map("currentPassword", PASSWORD, "newPassword", "a".repeat(72))).status());

        Person other = person("pwd");
        assertEquals(403, api.put(path, other.token(), map("currentPassword", PASSWORD, "newPassword", "NovaSenha@456")).status());
        assertEquals(401, api.put(path, null, map("currentPassword", PASSWORD, "newPassword", "NovaSenha@456")).status());
        assertEquals(200, login(other.email(), PASSWORD).status());
    }

    @Test
    @DisplayName("IT-ACC-03 E-mail change: request sends a code to the new address; confirming returns the user and keeps the session")
    void changeEmail() {
        Person user = person("mail");
        String target = newEmail();
        String path = "/api/v1/users/" + user.id() + "/email-change";

        Resp requested = api.post(path, user.token(), map("newEmail", target));

        assertEquals(200, requested.status(), requested.body());
        assertEquals("Enviamos um código de verificação para " + target + ".", requested.message());
        String code = MailCapture.emailChangeCode(target);
        assertNotNull(code);
        assertTrue(code.matches("\\d{6}"));
        // nothing changes before the confirmation
        assertEquals(user.email(), api.get("/api/v1/users/" + user.id(), user.token()).text("email"));

        Resp confirmed = api.post(path + "/confirm", user.token(), map("token", code));

        assertEquals(200, confirmed.status(), confirmed.body());
        assertEquals(user.id(), confirmed.num("id"));
        assertEquals(target, confirmed.text("email"));
        assertEquals("Mail Teste", confirmed.text("name"));
        assertTrue(confirmed.text("createdAt").endsWith("Z"));
        assertNull(confirmed.json().get("password"));
        // the JWT subject is the user id: the same token keeps working, and the new e-mail logs in
        assertEquals(200, api.get("/api/v1/users/" + user.id(), user.token()).status());
        assertEquals(200, login(target, PASSWORD).status());
        assertEquals(401, login(user.email(), PASSWORD).status());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM email_change_tokens WHERE user_id = ?", Integer.class, user.id()));
        // the code is single use
        assertEquals(400, api.post(path + "/confirm", user.token(), map("token", code)).status());
    }

    @Test
    @DisplayName("IT-ACC-04 E-mail change request: address already used 409; other user 403; invalid address 400")
    void requestErrors() {
        Person user = person("mail");
        String path = "/api/v1/users/" + user.id() + "/email-change";

        Resp used = api.post(path, user.token(), map("newEmail", carlos.email()));
        assertEquals(409, used.status());
        assertEquals("Este e-mail já está em uso.", used.message());
        assertEquals(409, api.post(path, user.token(), map("newEmail", user.email())).status());
        assertEquals(400, api.post(path, user.token(), map("newEmail", "nao-e-email")).status());
        assertEquals(403, api.post(path, carlos.token(), map("newEmail", newEmail())).status());
        assertEquals(403, api.post(path + "/confirm", carlos.token(), map("token", "123456")).status());
        assertEquals(401, api.post(path, null, map("newEmail", newEmail())).status());
    }

    @Test
    @DisplayName("IT-ACC-05 Wrong code: 400 per attempt; the 5th wrong attempt discards the code, even the right one then fails")
    void attemptsLimit() {
        Person user = person("mail");
        String target = newEmail();
        String path = "/api/v1/users/" + user.id() + "/email-change";
        api.post(path, user.token(), map("newEmail", target));
        String code = MailCapture.emailChangeCode(target);
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        for (int i = 1; i <= 4; i++) {
            Resp r = api.post(path + "/confirm", user.token(), map("token", wrongCode));
            assertEquals(400, r.status());
            assertEquals("Código inválido.", r.message());
            assertEquals(i, jdbc.queryForObject("SELECT attempts FROM email_change_tokens WHERE user_id = ?", Integer.class, user.id()));
        }
        Resp fifth = api.post(path + "/confirm", user.token(), map("token", wrongCode));
        assertEquals(400, fifth.status());
        assertTrue(fifth.message().startsWith("Muitas tentativas"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM email_change_tokens WHERE user_id = ?", Integer.class, user.id()));

        assertEquals(400, api.post(path + "/confirm", user.token(), map("token", code)).status());
        assertEquals(user.email(), api.get("/api/v1/users/" + user.id(), user.token()).text("email"));
    }

    @Test
    @DisplayName("IT-ACC-06 Expired code (10 minutes) is 400 and a new request replaces the previous code")
    void expiryAndReplacement() {
        Person user = person("mail");
        String path = "/api/v1/users/" + user.id() + "/email-change";
        String first = newEmail();
        api.post(path, user.token(), map("newEmail", first));
        String firstCode = MailCapture.emailChangeCode(first);
        long minutes = jdbc.queryForObject(
                "SELECT TIMESTAMPDIFF(MINUTE, UTC_TIMESTAMP(), expires_at) FROM email_change_tokens WHERE user_id = ?", Long.class, user.id());
        assertTrue(minutes >= 9 && minutes <= 10, "code lasts 10 minutes, was " + minutes);

        jdbc.update("UPDATE email_change_tokens SET expires_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 1 SECOND) WHERE user_id = ?", user.id());
        Resp expired = api.post(path + "/confirm", user.token(), map("token", firstCode));
        assertEquals(400, expired.status());
        assertEquals("Código inválido ou expirado. Solicite um novo.", expired.message());

        String second = newEmail();
        api.post(path, user.token(), map("newEmail", first));
        api.post(path, user.token(), map("newEmail", second));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM email_change_tokens WHERE user_id = ?", Integer.class, user.id()));
        // the code of the first request no longer matches the stored request (other address and code)
        String secondCode = MailCapture.emailChangeCode(second);
        Resp done = api.post(path + "/confirm", user.token(), map("token", secondCode));
        assertEquals(200, done.status(), done.body());
        assertEquals(second, done.text("email"));
    }

    @Test
    @DisplayName("IT-ACC-07 Address taken between request and confirmation is 409 and the code is discarded")
    void takenMeanwhile() {
        Person user = person("mail");
        String target = newEmail();
        String path = "/api/v1/users/" + user.id() + "/email-change";
        api.post(path, user.token(), map("newEmail", target));
        String code = MailCapture.emailChangeCode(target);
        jdbc.update("INSERT INTO users (email, password, name) VALUES (?, ?, 'Outro')", target, passwordEncoder.encode(PASSWORD));

        Resp r = api.post(path + "/confirm", user.token(), map("token", code));

        assertEquals(409, r.status());
        assertEquals(user.email(), api.get("/api/v1/users/" + user.id(), user.token()).text("email"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM email_change_tokens WHERE user_id = ?", Integer.class, user.id()));
    }
}
