-- YouTube post-publication tracking (decision 23): the link of the published video lives on the card,
-- and each refresh of the public statistics (YouTube Data API v3, API key, no OAuth) is stored as a snapshot.
-- Snapshots are removed with the card, and by the application when the link is cleared or changed to another video.
ALTER TABLE kanban_items
    ADD COLUMN video_url VARCHAR(500) NULL;

CREATE TABLE video_stat_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kanban_item_id BIGINT NOT NULL,
    video_id VARCHAR(11) NOT NULL,
    view_count BIGINT NOT NULL,
    like_count BIGINT NULL,
    comment_count BIGINT NULL,
    title VARCHAR(255) NULL,
    published_at TIMESTAMP NULL,
    captured_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_video_stat_snapshots_kanban_item FOREIGN KEY (kanban_item_id) REFERENCES kanban_items (id) ON DELETE CASCADE,
    INDEX idx_video_stat_snapshots_item_captured (kanban_item_id, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
