-- Card tasks can have a responsible team member and a due date
ALTER TABLE card_tasks
    ADD COLUMN assignee_member_id BIGINT NULL,
    ADD COLUMN due_date DATE NULL,
    ADD CONSTRAINT fk_card_tasks_assignee FOREIGN KEY (assignee_member_id) REFERENCES team_members (id) ON DELETE SET NULL;
