-- Invites by e-mail. Only the SHA-256 hash (hex) of the token is stored; the token itself travels in the e-mail link.
-- Expiry is computed (expires_at); status only tells whether the invite was already used.
CREATE TABLE team_invites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'CREATOR', 'REVIEWER') NOT NULL DEFAULT 'CREATOR',
    token_hash VARCHAR(64) NOT NULL,
    status ENUM('PENDING', 'ACCEPTED') NOT NULL DEFAULT 'PENDING',
    invited_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_team_invites_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_team_invites_invited_by FOREIGN KEY (invited_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT uk_team_invites_token_hash UNIQUE (token_hash),
    INDEX idx_team_invites_team_email (team_id, email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
