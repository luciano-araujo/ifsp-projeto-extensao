package br.edu.ifsp.tcc.application.service;

import java.time.LocalDateTime;

/** Public statistics and metadata of a YouTube video, as returned by {@link YouTubeClient}. Like and comment counts may be hidden (null). */
public record VideoStats(String videoId, long viewCount, Long likeCount, Long commentCount,
                         String title, LocalDateTime publishedAt) { }
