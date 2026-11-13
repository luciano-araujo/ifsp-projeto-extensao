package br.edu.ifsp.tcc.application.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Public statistics of the YouTube video of a card, captured at one moment (history of the post-publication tracking). */
@Entity
@Table(name = "video_stat_snapshots")
public class VideoStatSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kanban_item_id", nullable = false)
    private KanbanItem kanbanItem;

    @Column(name = "video_id", nullable = false, length = 11)
    private String videoId;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "like_count")
    private Long likeCount;

    @Column(name = "comment_count")
    private Long commentCount;

    @Column(length = 255)
    private String title;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KanbanItem getKanbanItem() { return kanbanItem; }
    public void setKanbanItem(KanbanItem kanbanItem) { this.kanbanItem = kanbanItem; }

    public Long getKanbanItemId() { return kanbanItem != null ? kanbanItem.getId() : null; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCapturedAt() { return capturedAt; }
    public void setCapturedAt(LocalDateTime capturedAt) { this.capturedAt = capturedAt; }
}
