package br.edu.ifsp.tcc.application.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QA M4: what the real EmailService puts on the wire (no network: the sender only captures the MIME message).
 * The e-mails are plain text, and a line break in a name can never become a header.
 */
class QaM4EmailContentTest {

    private final List<String> sent = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private final EmailService service;

    QaM4EmailContentTest() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl() {
            @Override
            protected void doSend(MimeMessage[] mimeMessages, Object[] originalMessages) {
                for (MimeMessage message : mimeMessages) {
                    try {
                        message.saveChanges();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        message.writeTo(out);
                        sent.add(out.toString(StandardCharsets.UTF_8));
                        bodies.add(String.valueOf(message.getContent()));
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        };
        sender.setSession(Session.getInstance(new Properties()));
        service = new EmailService(sender);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:3000/");
    }

    private static List<String> headerLines(String raw) {
        String headers = raw.substring(0, raw.indexOf("\r\n\r\n") >= 0 ? raw.indexOf("\r\n\r\n") : raw.indexOf("\n\n"));
        return List.of(headers.split("\r?\n"));
    }

    @Test
    @DisplayName("QA4-MAIL-01 The digest is text/plain: markup in names is sent as typed, nothing is turned into HTML")
    void digestIsPlainText() {
        service.sendDeadlineDigest("ana@creatorworkbench.test", "Ana <b>Teste</b>",
                List.of("[<script>alert(1)</script>] Projeto \"<img src=x onerror=alert(1)>\": vence hoje (13/11/2026)"));

        String raw = sent.get(0);
        assertTrue(headerLines(raw).stream().anyMatch(h -> h.toLowerCase().startsWith("content-type: text/plain")), raw);
        assertFalse(raw.toLowerCase().contains("text/html"));
        assertTrue(bodies.get(0).contains("<script>alert(1)</script>"));
    }

    @Test
    @DisplayName("QA4-MAIL-02 A team name with CR/LF cannot add headers (Bcc, Cc, Reply-To) to the invite e-mail")
    void teamNameCannotInjectHeaders() {
        service.sendTeamInvite("dora@creatorworkbench.test", "Ana", "Equipe\r\nBcc: attacker@creatorworkbench.test", "Criador", "token123");

        List<String> headers = headerLines(sent.get(0));
        assertTrue(headers.stream().noneMatch(h -> h.toLowerCase().startsWith("bcc:")), "injected header: " + headers);
        assertEquals(1, headers.stream().filter(h -> h.toLowerCase().startsWith("subject:")).count());
    }

    @Test
    @DisplayName("QA4-MAIL-03 The invite link carries the token to the /convite page of the frontend and nothing else")
    void inviteLink() {
        service.sendTeamInvite("dora@creatorworkbench.test", "Ana", "Equipe", "Criador", "abc-123_XYZ");

        assertTrue(bodies.get(0).contains("http://localhost:3000/convite?token=abc-123_XYZ"), bodies.get(0));
    }

    @Test
    @DisplayName("QA4-MAIL-04 The e-mail change code goes to the new address only")
    void emailChangeRecipient() {
        service.sendEmailChangeToken("novo@creatorworkbench.test", "Ana", "123456");

        List<String> headers = headerLines(sent.get(0));
        assertTrue(headers.stream().anyMatch(h -> h.equalsIgnoreCase("To: novo@creatorworkbench.test")), headers.toString());
        assertTrue(bodies.get(0).contains("123456"));
        assertTrue(headers.stream().noneMatch(h -> h.toLowerCase().startsWith("cc:") || h.toLowerCase().startsWith("bcc:")));
    }
}
