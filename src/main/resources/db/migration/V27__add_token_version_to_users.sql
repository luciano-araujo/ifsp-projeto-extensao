-- Sessions: the JWT carries the user's token_version; a password change or reset increments it and invalidates the other sessions.
ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0;
