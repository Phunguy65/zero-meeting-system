-- V5: Google auth support (H2-compatible)

ALTER TABLE users
    ALTER COLUMN password_hash VARCHAR(255) NULL;

ALTER TABLE users
    ADD COLUMN google_uid VARCHAR(128);

ALTER TABLE users
    ADD CONSTRAINT uq_users_google_uid UNIQUE (google_uid);

ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'EMAIL';
