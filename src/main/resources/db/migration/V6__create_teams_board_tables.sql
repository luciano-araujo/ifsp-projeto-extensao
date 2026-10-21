CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM('ADMIN', 'CREATOR', 'REVIEWER') NOT NULL DEFAULT 'CREATOR',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_members_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_team_members_team_user UNIQUE (team_id, user_id),
    INDEX idx_team_members_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE board_columns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_board_columns_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    INDEX idx_board_columns_team_id (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE kanban_items
    ADD COLUMN team_id BIGINT NULL,
    ADD COLUMN column_id BIGINT NULL,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_kanban_items_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_kanban_items_column FOREIGN KEY (column_id) REFERENCES board_columns (id) ON DELETE SET NULL,
    ADD INDEX idx_kanban_items_team_id (team_id);

CREATE TABLE card_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kanban_item_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_tasks_kanban_item FOREIGN KEY (kanban_item_id) REFERENCES kanban_items (id) ON DELETE CASCADE,
    INDEX idx_card_tasks_kanban_item_id (kanban_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
