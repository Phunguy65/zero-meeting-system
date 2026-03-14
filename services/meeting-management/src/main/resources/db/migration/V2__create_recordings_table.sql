-- V2: Create recordings table
CREATE TABLE recordings
(
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    meeting_id       UUID        NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    file_url         VARCHAR(2048),
    thumbnail_url    VARCHAR(2048),
    status           VARCHAR(20) NOT NULL DEFAULT 'RECORDING'
        CHECK (status IN ('RECORDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    started_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at         TIMESTAMPTZ,
    duration_seconds INT         NOT NULL DEFAULT 0,
    file_size_bytes  BIGINT      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recordings_meeting_id ON recordings (meeting_id);
CREATE INDEX idx_recordings_status     ON recordings (status);

-- Enforce at most one active (RECORDING or PROCESSING) recording per meeting
CREATE UNIQUE INDEX uq_recordings_active_per_meeting
    ON recordings (meeting_id)
    WHERE status IN ('RECORDING', 'PROCESSING');
