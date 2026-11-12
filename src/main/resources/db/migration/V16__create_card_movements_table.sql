-- History of the columns a card has been in (the column name is kept as a snapshot)
CREATE TABLE card_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kanban_item_id BIGINT NOT NULL,
    column_id BIGINT NULL,
    column_name VARCHAR(120) NOT NULL,
    moved_by BIGINT NULL,
    entered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_movements_kanban_item FOREIGN KEY (kanban_item_id) REFERENCES kanban_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_card_movements_column FOREIGN KEY (column_id) REFERENCES board_columns (id) ON DELETE SET NULL,
    CONSTRAINT fk_card_movements_user FOREIGN KEY (moved_by) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_card_movements_item_entered (kanban_item_id, entered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- One row per existing project card: the column it is in now (a card without column shows in the first one),
-- entered at the card's last update
INSERT INTO card_movements (kanban_item_id, column_id, column_name, moved_by, entered_at)
SELECT k.id, c.id, c.name, NULL, k.updated_at
FROM kanban_items k
JOIN board_columns c ON c.id = COALESCE(
    k.column_id,
    (SELECT c2.id FROM board_columns c2 WHERE c2.team_id = k.team_id ORDER BY c2.sort_order, c2.id LIMIT 1))
WHERE k.team_id IS NOT NULL
  AND k.state <> 'IDEATION';
