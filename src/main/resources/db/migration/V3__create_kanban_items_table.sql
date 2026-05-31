CREATE TABLE kanban_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    state ENUM('IDEATION', 'IN_PRODUCTION', 'REVIEW', 'DONE') NOT NULL DEFAULT 'IDEATION',
    target_audience VARCHAR(255),
    pedagogical_objective TEXT,
    progress INT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_kanban_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_kanban_items_user_id (user_id),
    INDEX idx_kanban_items_state (state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
