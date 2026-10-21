ALTER TABLE registration_tokens
    ADD COLUMN attempts INT NOT NULL DEFAULT 0;
