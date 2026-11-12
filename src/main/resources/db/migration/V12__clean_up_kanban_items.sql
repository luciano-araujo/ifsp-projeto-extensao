-- Progress is computed from the card tasks, no longer stored
ALTER TABLE kanban_items DROP COLUMN progress;

-- State is only the idea/project flag: REVIEW and DONE become IN_PRODUCTION
-- (updated_at = updated_at keeps the ON UPDATE CURRENT_TIMESTAMP column unchanged)
UPDATE kanban_items SET state = 'IN_PRODUCTION', updated_at = updated_at WHERE state IN ('REVIEW', 'DONE');
ALTER TABLE kanban_items MODIFY COLUMN state ENUM('IDEATION', 'IN_PRODUCTION') NOT NULL DEFAULT 'IDEATION';

-- Authors with team-less cards and no team get a personal team, like TeamService.createTeamWithAdmin:
-- team "Estúdio de <first name>", the author as ADMIN and the four default columns.
-- teams.tmp_user_id is a temporary column that maps each new team to its author; it is dropped at the end.
ALTER TABLE teams ADD COLUMN tmp_user_id BIGINT NULL;

INSERT INTO teams (name, tmp_user_id)
SELECT CASE WHEN TRIM(u.name) = '' THEN 'Meu Estudio'
            ELSE CONCAT('Estúdio de ', SUBSTRING_INDEX(TRIM(u.name), ' ', 1)) END,
       u.id
FROM users u
WHERE EXISTS (SELECT 1 FROM kanban_items k WHERE k.user_id = u.id AND k.team_id IS NULL)
  AND NOT EXISTS (SELECT 1 FROM team_members tm WHERE tm.user_id = u.id);

INSERT INTO team_members (team_id, user_id, role)
SELECT t.id, t.tmp_user_id, 'ADMIN' FROM teams t WHERE t.tmp_user_id IS NOT NULL;

INSERT INTO board_columns (team_id, name, sort_order)
SELECT t.id, c.name, c.sort_order
FROM teams t
JOIN (SELECT 'Pesquisa' AS name, 0 AS sort_order
      UNION ALL SELECT 'Roteiro', 1
      UNION ALL SELECT 'Gravação', 2
      UNION ALL SELECT 'Edição', 3) c
WHERE t.tmp_user_id IS NOT NULL;

-- Every card belongs to a team: cards without one go to their author's oldest team (the personal team)
UPDATE kanban_items k
SET k.team_id = (
    SELECT tm.team_id FROM team_members tm
    WHERE tm.user_id = k.user_id
    ORDER BY tm.joined_at ASC, tm.id ASC
    LIMIT 1),
    k.updated_at = k.updated_at
WHERE k.team_id IS NULL AND k.user_id IS NOT NULL;

-- Only cards without an author (account already deleted) and without a team are removed,
-- with their tasks, script and votes
DELETE FROM kanban_items WHERE team_id IS NULL;

-- Projects not yet on the board go to the first column of their team
UPDATE kanban_items k
SET k.column_id = (
    SELECT bc.id FROM board_columns bc
    WHERE bc.team_id = k.team_id
    ORDER BY bc.sort_order ASC, bc.id ASC
    LIMIT 1),
    k.updated_at = k.updated_at
WHERE k.state = 'IN_PRODUCTION' AND k.column_id IS NULL;

ALTER TABLE kanban_items MODIFY COLUMN team_id BIGINT NOT NULL;

ALTER TABLE teams DROP COLUMN tmp_user_id;
