-- Team tags: replace the free-text kanban_items.tag by a reference to a per-team tag (name + color)
CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    name VARCHAR(40) NOT NULL,
    color CHAR(7) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tags_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT uk_tags_team_name UNIQUE (team_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- The 5 default tags for every existing team
INSERT INTO tags (team_id, name, color)
SELECT t.id, d.name, d.color
FROM teams t
CROSS JOIN (
    SELECT 'Tutorial' AS name, '#2E7D32' AS color
    UNION ALL SELECT 'Podcast', '#1565C0'
    UNION ALL SELECT 'Curso', '#F57C00'
    UNION ALL SELECT 'Roteiro', '#7B1FA2'
    UNION ALL SELECT 'Artigo', '#C2185B'
) d;

-- Free-text tags already used by cards that are not among the defaults of their team (neutral color)
INSERT INTO tags (team_id, name, color)
SELECT DISTINCT k.team_id, TRIM(k.tag), '#607D8B'
FROM kanban_items k
WHERE k.team_id IS NOT NULL
  AND k.tag IS NOT NULL
  AND TRIM(k.tag) <> ''
  AND NOT EXISTS (SELECT 1 FROM tags g WHERE g.team_id = k.team_id AND g.name = TRIM(k.tag));

ALTER TABLE kanban_items
    ADD COLUMN tag_id BIGINT NULL,
    ADD CONSTRAINT fk_kanban_items_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE SET NULL;

UPDATE kanban_items k
    JOIN tags g ON g.team_id = k.team_id AND g.name = TRIM(k.tag)
SET k.tag_id = g.id
WHERE k.tag IS NOT NULL;

ALTER TABLE kanban_items DROP COLUMN tag;
