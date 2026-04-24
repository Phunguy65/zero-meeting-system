package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;

/** Recording metadata used for playback in meeting detail. */
public record MeetingRecording(
        String id, String fileUrl, Integer durationSeconds, OffsetDateTime createdAt) {}
