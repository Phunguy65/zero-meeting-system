package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;

/** Summary model for a single meeting in the history list. */
public record MeetingHistory(
        String id,
        String title,
        String description,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        MeetingType type,
        MeetingStatus status) {}
