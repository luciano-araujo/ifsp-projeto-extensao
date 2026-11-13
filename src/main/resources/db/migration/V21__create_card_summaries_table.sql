-- Last summary generated from the script of a project card (contract 7.11); overwritten on regenerate
CREATE TABLE card_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kanban_item_id BIGINT NOT NULL,
    summary TEXT NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_card_summaries_kanban_item UNIQUE (kanban_item_id),
    CONSTRAINT fk_card_summaries_kanban_item FOREIGN KEY (kanban_item_id) REFERENCES kanban_items (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
