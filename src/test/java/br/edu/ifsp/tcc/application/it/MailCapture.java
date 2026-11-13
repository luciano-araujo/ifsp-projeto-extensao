package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.service.EmailService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Replaces the e-mail sender in M4 integration tests: nothing is sent, the content is kept for assertions. */
public final class MailCapture {

    /** Latest invite token sent to each address (lower case). */
    public static final Map<String, String> INVITE_TOKENS = new ConcurrentHashMap<>();
    /** Latest e-mail-change code sent to each address (lower case). */
    public static final Map<String, String> EMAIL_CHANGE_CODES = new ConcurrentHashMap<>();
    /** Every digest sent, in order: address (lower case) -> lines. */
    public static final List<Digest> DIGESTS = new CopyOnWriteArrayList<>();

    public record Digest(String to, String name, List<String> lines) { }

    private MailCapture() {
    }

    // The service methods are @Async (the proxy keeps that on the overriding methods), so the capture is
    // filled from another thread: tests wait for it instead of reading right after the request.

    public static String inviteToken(String email) {
        return await(INVITE_TOKENS, email.toLowerCase());
    }

    public static String emailChangeCode(String email) {
        return await(EMAIL_CHANGE_CODES, email.toLowerCase());
    }

    /** Waits until at least the given number of digests were captured. */
    public static void awaitDigests(int expected) {
        long deadline = System.currentTimeMillis() + 5000;
        while (DIGESTS.size() < expected && System.currentTimeMillis() < deadline) {
            sleep();
        }
    }

    private static String await(Map<String, String> map, String key) {
        long deadline = System.currentTimeMillis() + 5000;
        while (!map.containsKey(key) && System.currentTimeMillis() < deadline) {
            sleep();
        }
        return map.get(key);
    }

    private static void sleep() {
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static List<Digest> digestsTo(String email) {
        return DIGESTS.stream().filter(d -> d.to().equals(email.toLowerCase())).toList();
    }

    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        EmailService capturingEmailService() {
            return new EmailService(null) {
                @Override
                public void sendTeamInvite(String to, String inviterName, String teamName, String roleLabel, String token) {
                    INVITE_TOKENS.put(to.toLowerCase(), token);
                }

                @Override
                public void sendEmailChangeToken(String to, String name, String token) {
                    EMAIL_CHANGE_CODES.put(to.toLowerCase(), token);
                }

                @Override
                public void sendDeadlineDigest(String to, String name, List<String> lines) {
                    DIGESTS.add(new Digest(to.toLowerCase(), name, List.copyOf(lines)));
                }
            };
        }
    }
}
