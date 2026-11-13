-- The personal team (created on the first access of a user) cannot be deleted.
ALTER TABLE teams ADD COLUMN personal BOOLEAN NOT NULL DEFAULT FALSE;

-- Existing data. A team is personal when its administrator is a user whose personal name it carries
-- (TeamService.personalTeamName: 'Estúdio de <first name>', or 'Meu Estúdio' for a blank name), or when it has an
-- automatic personal name and is the earliest membership of that administrator (covers a renamed account).
UPDATE teams t
    JOIN team_members tm ON tm.team_id = t.id AND tm.role = 'ADMIN'
    JOIN users u ON u.id = tm.user_id
SET t.personal = TRUE
WHERE t.name = CASE WHEN TRIM(u.name) = '' THEN 'Meu Estúdio'
                    ELSE CONCAT('Estúdio de ', SUBSTRING_INDEX(TRIM(u.name), ' ', 1)) END
   OR ((t.name = 'Meu Estúdio' OR t.name LIKE 'Estúdio de %')
       AND NOT EXISTS (SELECT 1 FROM team_members e WHERE e.user_id = tm.user_id
                       AND (e.joined_at < tm.joined_at OR (e.joined_at = tm.joined_at AND e.id < tm.id))));
