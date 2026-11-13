-- The personal team (created on the first access of a user) cannot be deleted.
ALTER TABLE teams ADD COLUMN personal BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing data: the earliest team of each user, when it carries the automatic personal name.
UPDATE teams t
    JOIN (SELECT MIN(team_id) AS team_id FROM team_members GROUP BY user_id) f ON f.team_id = t.id
SET t.personal = TRUE
WHERE t.name = 'Meu Estúdio' OR t.name LIKE 'Estúdio de %';
