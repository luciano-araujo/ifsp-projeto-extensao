-- Team cards belong to the team: deleting the author's account keeps the card and only removes the author link
ALTER TABLE kanban_items DROP FOREIGN KEY fk_kanban_items_user;

ALTER TABLE kanban_items MODIFY COLUMN user_id BIGINT NULL;

ALTER TABLE kanban_items
    ADD CONSTRAINT fk_kanban_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;
