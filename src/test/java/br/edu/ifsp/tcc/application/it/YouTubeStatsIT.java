package br.edu.ifsp.tcc.application.it;

import br.edu.ifsp.tcc.application.service.VideoStats;
import br.edu.ifsp.tcc.application.service.YouTubeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static br.edu.ifsp.tcc.application.it.Api.map;
import static org.junit.jupiter.api.Assertions.*;

/** YouTube post-publication tracking (decision 23). The YouTube client is a stub bean: the real API is never called. */
class YouTubeStatsIT extends AbstractIT {

    private static final String VIDEO = "dQw4w9WgXcQ";
    private static final String URL = "https://www.youtube.com/watch?v=" + VIDEO;
    private static final String OTHER_URL = "https://youtu.be/aaaaaaaaaaa";

    static final AtomicBoolean CONFIGURED = new AtomicBoolean(true);
    static final AtomicBoolean DOWN = new AtomicBoolean(false);
    static final AtomicInteger CALLS = new AtomicInteger();
    static final AtomicReference<Long> NEXT_VIEWS = new AtomicReference<>(100L);
    /** Ids the stub knows; others come back missing (private or deleted video). */
    static final Map<String, Boolean> KNOWN = new HashMap<>();

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        YouTubeClient stubYouTubeClient() {
            return new YouTubeClient() {
                @Override
                public boolean isConfigured() {
                    return CONFIGURED.get();
                }

                @Override
                public Map<String, VideoStats> fetch(List<String> videoIds) {
                    CALLS.incrementAndGet();
                    if (DOWN.get()) {
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Não foi possível consultar o YouTube agora. Tente novamente.");
                    }
                    Map<String, VideoStats> result = new HashMap<>();
                    for (String id : videoIds) {
                        if (KNOWN.getOrDefault(id, true)) {
                            result.put(id, new VideoStats(id, NEXT_VIEWS.get(), 10L, null, "Aula " + id,
                                    LocalDateTime.of(2024, 3, 5, 10, 20, 30)));
                        }
                    }
                    return result;
                }
            };
        }
    }

    @BeforeEach
    void reset() {
        CONFIGURED.set(true);
        DOWN.set(false);
        CALLS.set(0);
        NEXT_VIEWS.set(100L);
        KNOWN.clear();
    }

    private String videoPath(Team team, long cardId) {
        return team.path() + "/cards/" + cardId + "/video";
    }

    private String statsPath(Team team, long cardId) {
        return videoPath(team, cardId) + "-stats";
    }

    private long linkedProject(Team team, String title) {
        long card = projectId(team, title);
        Resp r = api.put(videoPath(team, card), ana.token(), map("url", URL));
        assertEquals(200, r.status(), r.body());
        return card;
    }

    @Test
    @DisplayName("IT-YT-01 PUT video: ADMIN and CREATOR link, response carries videoUrl; board shows it; blank clears")
    void linkAndUnlink() {
        Team team = newTeam();
        long card = projectId(team, "Com vídeo");

        Resp linked = api.put(videoPath(team, card), carlos.token(), map("url", "  " + URL + " "));
        assertEquals(200, linked.status(), linked.body());
        assertEquals(URL, linked.text("videoUrl"));
        assertEquals(card, linked.num("id"));
        assertEquals("PROJECT", linked.text("type"));

        Resp board = api.get(team.path() + "/board", ana.token());
        boolean found = false;
        for (var column : board.at("columns")) {
            for (var c : column.path("cards")) {
                if (c.path("id").asLong() == card) {
                    assertEquals(URL, c.path("videoUrl").asText());
                    found = true;
                }
            }
        }
        assertTrue(found);

        Resp cleared = api.put(videoPath(team, card), ana.token(), map("url", "   "));
        assertEquals(200, cleared.status(), cleared.body());
        assertTrue(cleared.at("videoUrl").isNull());
        assertEquals(404, api.get(statsPath(team, card), ana.token()).status());
    }

    @Test
    @DisplayName("IT-YT-02 PUT video: REVIEWER 403, outsider 403, anonymous 401, invalid link 400, idea 400")
    void putRulesAndRoles() {
        Team team = newTeam();
        long card = projectId(team, "Regras");

        assertEquals(403, api.put(videoPath(team, card), rui.token(), map("url", URL)).status());
        assertEquals(403, api.put(videoPath(team, card), bia.token(), map("url", URL)).status());
        assertEquals(401, api.put(videoPath(team, card), null, map("url", URL)).status());

        for (String bad : new String[]{"https://vimeo.com/123456", "http://youtu.be/" + VIDEO, "https://youtu.be/curto", "texto"}) {
            Resp r = api.put(videoPath(team, card), ana.token(), map("url", bad));
            assertEquals(400, r.status(), bad + " -> " + r.body());
            assertEquals("Informe um link válido do YouTube.", r.message());
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM kanban_items WHERE id = ? AND video_url IS NOT NULL",
                Integer.class, card));

        long idea = ideaId(team, "Ideia sem vídeo");
        Resp onIdea = api.put(videoPath(team, idea), ana.token(), map("url", URL));
        assertEquals(400, onIdea.status(), onIdea.body());
    }

    @Test
    @DisplayName("IT-YT-03 GET video-stats: 404 without link; any member reads after linking; outsider 403")
    void getStats() {
        Team team = newTeam();
        long card = projectId(team, "Sem link");

        Resp none = api.get(statsPath(team, card), ana.token());
        assertEquals(404, none.status());
        assertEquals("Este projeto não tem vídeo do YouTube vinculado.", none.message());

        assertEquals(200, api.put(videoPath(team, card), ana.token(), map("url", URL)).status());
        for (Person p : new Person[]{ana, carlos, rui}) {
            Resp got = api.get(statsPath(team, card), p.token());
            assertEquals(200, got.status(), got.body());
            assertEquals(VIDEO, got.text("videoId"));
            assertEquals(URL, got.text("videoUrl"));
            assertTrue(got.at("latest").isNull());
            assertEquals(0, got.at("history").size());
        }
        assertEquals(403, api.get(statsPath(team, card), bia.token()).status());
        assertEquals(401, api.get(statsPath(team, card), null).status());
    }

    @Test
    @DisplayName("IT-YT-04 Refresh saves a snapshot; within 15 minutes the cache is returned without calling the client")
    void refreshAndCache() {
        Team team = newTeam();
        long card = linkedProject(team, "Atualizável");

        Resp first = api.post(statsPath(team, card) + "/refresh", rui.token(), null);
        assertEquals(200, first.status(), first.body());
        assertEquals(1, CALLS.get());
        assertEquals(VIDEO, first.text("videoId"));
        assertEquals("Aula " + VIDEO, first.text("title"));
        assertEquals("2024-03-05T10:20:30Z", first.text("publishedAt"));
        assertEquals(100L, first.at("latest").path("viewCount").asLong());
        assertEquals(10L, first.at("latest").path("likeCount").asLong());
        assertTrue(first.at("latest").path("commentCount").isNull());
        assertTrue(first.text("latest.capturedAt").endsWith("Z"));
        assertEquals(1, first.at("history").size());

        NEXT_VIEWS.set(999L);
        Resp cached = api.post(statsPath(team, card) + "/refresh", carlos.token(), null);
        assertEquals(200, cached.status(), cached.body());
        assertEquals(1, CALLS.get(), "the client is not called within the cache window");
        assertEquals(100L, cached.at("latest").path("viewCount").asLong());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM video_stat_snapshots WHERE kanban_item_id = ?", Integer.class, card));

        jdbc.update("UPDATE video_stat_snapshots SET captured_at = captured_at - INTERVAL 20 MINUTE WHERE kanban_item_id = ?", card);
        Resp renewed = api.post(statsPath(team, card) + "/refresh", ana.token(), null);
        assertEquals(200, renewed.status(), renewed.body());
        assertEquals(2, CALLS.get());
        assertEquals(999L, renewed.at("latest").path("viewCount").asLong());
        assertEquals(2, renewed.at("history").size());
    }

    @Test
    @DisplayName("IT-YT-05 History: oldest first, latest is the newest, at most 60 entries")
    void historyOrderAndLimit() {
        Team team = newTeam();
        long card = linkedProject(team, "Histórico");
        for (int i = 0; i < 65; i++) {
            jdbc.update("INSERT INTO video_stat_snapshots (kanban_item_id, video_id, view_count, title, captured_at) "
                    + "VALUES (?, ?, ?, ?, ?)", card, VIDEO, 1000 + i, "Aula", LocalDateTime.of(2025, 1, 1, 0, 0).plusHours(i));
        }

        Resp got = api.get(statsPath(team, card), ana.token());
        assertEquals(200, got.status(), got.body());
        assertEquals(60, got.at("history").size());
        assertEquals(1005L, got.at("history").get(0).path("viewCount").asLong());
        assertEquals(1064L, got.at("history").get(59).path("viewCount").asLong());
        assertEquals(1064L, got.at("latest").path("viewCount").asLong());
        assertTrue(got.at("history").get(0).path("capturedAt").asText().compareTo(
                got.at("history").get(1).path("capturedAt").asText()) < 0);
    }

    @Test
    @DisplayName("IT-YT-06 Changing to another video clears the snapshots; same video keeps them; unlinking clears them")
    void snapshotsFollowTheLink() {
        Team team = newTeam();
        long card = linkedProject(team, "Troca");
        assertEquals(200, api.post(statsPath(team, card) + "/refresh", ana.token(), null).status());
        String count = "SELECT COUNT(*) FROM video_stat_snapshots WHERE kanban_item_id = ?";
        assertEquals(1, jdbc.queryForObject(count, Integer.class, card));

        assertEquals(200, api.put(videoPath(team, card), ana.token(), map("url", "https://youtu.be/" + VIDEO + "?si=x")).status());
        assertEquals(1, jdbc.queryForObject(count, Integer.class, card), "same video: the history stays");

        assertEquals(200, api.put(videoPath(team, card), ana.token(), map("url", OTHER_URL)).status());
        assertEquals(0, jdbc.queryForObject(count, Integer.class, card), "another video: the old snapshots go away");

        assertEquals(200, api.post(statsPath(team, card) + "/refresh", ana.token(), null).status());
        assertEquals(1, jdbc.queryForObject(count, Integer.class, card));
        assertEquals(200, api.put(videoPath(team, card), ana.token(), map("url", "")).status());
        assertEquals(0, jdbc.queryForObject(count, Integer.class, card), "unlinking clears the history");
    }

    @Test
    @DisplayName("IT-YT-07 Refresh errors: no link 404, video not found 404, no key 503, YouTube down 503; nothing is saved")
    void refreshErrors() {
        Team team = newTeam();
        long noLink = projectId(team, "Sem link");
        Resp r = api.post(statsPath(team, noLink) + "/refresh", ana.token(), null);
        assertEquals(404, r.status());
        assertEquals("Este projeto não tem vídeo do YouTube vinculado.", r.message());

        long card = linkedProject(team, "Falhas");
        KNOWN.put(VIDEO, false);
        Resp missing = api.post(statsPath(team, card) + "/refresh", ana.token(), null);
        assertEquals(404, missing.status(), missing.body());
        assertEquals("Vídeo não encontrado ou privado no YouTube.", missing.message());

        KNOWN.clear();
        CONFIGURED.set(false);
        int before = CALLS.get();
        Resp noKey = api.post(statsPath(team, card) + "/refresh", ana.token(), null);
        assertEquals(503, noKey.status(), noKey.body());
        assertEquals("Integração com o YouTube não configurada.", noKey.message());
        assertEquals(before, CALLS.get());

        CONFIGURED.set(true);
        DOWN.set(true);
        Resp down = api.post(statsPath(team, card) + "/refresh", ana.token(), null);
        assertEquals(503, down.status(), down.body());
        assertEquals("Não foi possível consultar o YouTube agora. Tente novamente.", down.message());

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM video_stat_snapshots WHERE kanban_item_id = ?", Integer.class, card));
        assertEquals(401, api.post(statsPath(team, card) + "/refresh", null, null).status());
        assertEquals(403, api.post(statsPath(team, card) + "/refresh", bia.token(), null).status());
    }

    @Test
    @DisplayName("IT-YT-08 Deleting the card removes its snapshots (cascade)")
    void cascadeOnCardDelete() {
        Team team = newTeam();
        long card = linkedProject(team, "Descartável");
        assertEquals(200, api.post(statsPath(team, card) + "/refresh", ana.token(), null).status());

        assertEquals(204, api.delete(team.path() + "/cards/" + card, ana.token()).status());

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM video_stat_snapshots WHERE kanban_item_id = ?", Integer.class, card));
    }

    @Test
    @DisplayName("IT-YT-09 Another team's card (or unknown id) answers 404 on every route, for a member of the path's team")
    void otherTeamsCard() {
        Team team = newTeam();
        long mine = linkedProject(team, "Da equipe A");
        Team other = otherTeam;

        assertEquals(404, api.put(videoPath(other, mine), bia.token(), map("url", URL)).status());
        assertEquals(404, api.get(statsPath(other, mine), bia.token()).status());
        assertEquals(404, api.post(statsPath(other, mine) + "/refresh", bia.token(), null).status());
        assertEquals(404, api.get(statsPath(team, 999999999L), ana.token()).status());
        assertEquals(0, CALLS.get());
    }
}
