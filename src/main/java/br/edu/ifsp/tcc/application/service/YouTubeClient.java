package br.edu.ifsp.tcc.application.service;

import java.util.List;
import java.util.Map;

/** Reads the public statistics of YouTube videos (Data API v3 with an API key; no OAuth). */
public interface YouTubeClient {

    /** False when no API key is configured: the feature then answers 503 and the daily job is skipped. */
    boolean isConfigured();

    /**
     * Statistics of up to 50 videos in one call. Videos that do not exist or are private are absent from the result.
     *
     * @throws org.springframework.web.server.ResponseStatusException 503 on any HTTP error, quota or timeout
     */
    Map<String, VideoStats> fetch(List<String> videoIds);
}
