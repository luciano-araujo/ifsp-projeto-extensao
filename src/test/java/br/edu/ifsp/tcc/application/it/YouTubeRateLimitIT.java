package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.service.VideoStats;
import br.edu.ifsp.tcc.application.service.YouTubeClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** Refreshes that reach YouTube are limited per team and hour (app.youtube.refresh-per-team-hour, 2 here); cache hits are free. */
@TestPropertySource(properties = "app.youtube.refresh-per-team-hour=2")
class YouTubeRateLimitIT extends AbstractIT {

    static final AtomicInteger CALLS = new AtomicInteger();

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        YouTubeClient countingYouTubeClient() {
            return new YouTubeClient() {
                @Override
                public boolean isConfigured() {
                    return true;
                }

                @Override
                public Map<String, VideoStats> fetch(List<String> videoIds) {
                    CALLS.incrementAndGet();
                    Map<String, VideoStats> result = new HashMap<>();
                    videoIds.forEach(id -> result.put(id, new VideoStats(id, 1, 1L, 1L, "t", LocalDateTime.of(2024, 1, 1, 0, 0))));
                    return result;
                }
            };
        }
    }

    private long linked(Team team, String title, String url) {
        long card = projectId(team, title);
        assertEquals(200, api.put(team.path() + "/cards/" + card + "/video", ana.token(), map("url", url)).status());
        return card;
    }

    @Test
    @DisplayName("IT-YT-RL-01 The 3rd refresh that reaches YouTube in the hour is 429 with Retry-After; cache hits and other teams are free")
    void limitPerTeam() {
        Team team = newTeam();
        Team other = newTeam();
        long c1 = linked(team, "RL 1", "https://youtu.be/aaaaaaaaaaa");
        long c2 = linked(team, "RL 2", "https://youtu.be/bbbbbbbbbbb");
        long c3 = linked(team, "RL 3", "https://youtu.be/ccccccccccc");
        long otherCard = linked(other, "RL outra", "https://youtu.be/ddddddddddd");
        String refresh = "/video-stats/refresh";
        int before = CALLS.get();

        assertEquals(200, api.post(team.path() + "/cards/" + c1 + refresh, ana.token(), null).status());
        assertEquals(200, api.post(team.path() + "/cards/" + c1 + refresh, ana.token(), null).status(), "cache hit: not counted");
        assertEquals(200, api.post(team.path() + "/cards/" + c2 + refresh, ana.token(), null).status());

        Resp limited = api.post(team.path() + "/cards/" + c3 + refresh, carlos.token(), null);
        assertEquals(429, limited.status(), limited.body());
        assertEquals("Limite de atualizações do YouTube atingido para esta equipe. Tente novamente mais tarde.", limited.message());
        assertNotNull(limited.header("Retry-After"));
        assertEquals(before + 2, CALLS.get());

        assertEquals(200, api.post(team.path() + "/cards/" + c1 + refresh, ana.token(), null).status(), "cached data is still served");
        assertEquals(200, api.post(other.path() + "/cards/" + otherCard + refresh, ana.token(), null).status());
    }
}
