ALTER TABLE kanban_items
    ADD COLUMN tag VARCHAR(40) NULL,
    ADD COLUMN due_date DATE NULL;

CREATE TABLE idea_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kanban_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_idea_votes_kanban_item FOREIGN KEY (kanban_item_id) REFERENCES kanban_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_idea_votes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_idea_votes_item_user UNIQUE (kanban_item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(6) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_password_reset_tokens_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
