-- Progress is computed from the card tasks, no longer stored
ALTER TABLE kanban_items DROP COLUMN progress;

-- State is only the idea/project flag: REVIEW and DONE become IN_PRODUCTION
UPDATE kanban_items SET state = 'IN_PRODUCTION' WHERE state IN ('REVIEW', 'DONE');
ALTER TABLE kanban_items MODIFY COLUMN state ENUM('IDEATION', 'IN_PRODUCTION') NOT NULL DEFAULT 'IDEATION';

-- Every card belongs to a team. Cards without a team go to their author's oldest team (the personal team);
-- cards that still have no team (author deleted or never in a team) are removed with their tasks, script and votes.
UPDATE kanban_items k
SET k.team_id = (
    SELECT tm.team_id FROM team_members tm
    WHERE tm.user_id = k.user_id
    ORDER BY tm.joined_at ASC, tm.id ASC
    LIMIT 1)
WHERE k.team_id IS NULL AND k.user_id IS NOT NULL;

DELETE FROM kanban_items WHERE team_id IS NULL;

ALTER TABLE kanban_items MODIFY COLUMN team_id BIGINT NOT NULL;
