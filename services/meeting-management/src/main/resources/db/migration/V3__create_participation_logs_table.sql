-- V3: Create participation_logs table
CREATE TABLE participation_logs
(
    id           BIGSERIAL    NOT NULL PRIMARY KEY,
    meeting_id   UUID         NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    user_id      UUID,                         -- NULL for guest participants
    display_name VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('HOST', 'PARTICIPANT')),
    joined_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    left_at      TIMESTAMPTZ,
    device_id    VARCHAR(255)
);

CREATE INDEX idx_participation_logs_meeting_id ON participation_logs (meeting_id);
CREATE INDEX idx_participation_logs_user_id    ON participation_logs (user_id)
    WHERE user_id IS NOT NULL;
CREATE INDEX idx_participation_logs_joined_at  ON participation_logs (joined_at DESC);

-- Partial index to quickly find active sessions (not yet left) per meeting + device
CREATE INDEX idx_participation_logs_active
    ON participation_logs (meeting_id, device_id)
    WHERE left_at IS NULL;
