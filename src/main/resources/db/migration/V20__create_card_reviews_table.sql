-- Reviews of a project card (contract 7.10): the reviewer is kept as SET NULL when the account is deleted
CREATE TABLE card_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kanban_item_id BIGINT NOT NULL,
    reviewer_id BIGINT NULL,
    decision ENUM('APPROVED', 'CHANGES_REQUESTED') NOT NULL,
    comment TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_reviews_kanban_item FOREIGN KEY (kanban_item_id) REFERENCES kanban_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_card_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_card_reviews_item_created (kanban_item_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
