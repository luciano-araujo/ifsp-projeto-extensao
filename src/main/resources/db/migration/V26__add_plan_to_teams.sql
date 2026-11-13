-- Plans (final delivery): every existing team starts on Gratuito. Trial expiry is computed on read (no job).
ALTER TABLE teams
    ADD COLUMN plan ENUM('GRATUITO', 'SOLO', 'PRO', 'AGENCIA') NOT NULL DEFAULT 'GRATUITO',
    ADD COLUMN plan_source ENUM('FREE', 'TRIAL', 'MANUAL') NOT NULL DEFAULT 'FREE',
    ADD COLUMN plan_expires_at TIMESTAMP NULL DEFAULT NULL,
    ADD COLUMN trial_used_at TIMESTAMP NULL DEFAULT NULL;
