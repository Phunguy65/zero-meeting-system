package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;

/** Domain entity representing a calendar event entry. */
public record CalendarEvent(
        String id,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        MeetingStatus status,
        MeetingType type) {}
