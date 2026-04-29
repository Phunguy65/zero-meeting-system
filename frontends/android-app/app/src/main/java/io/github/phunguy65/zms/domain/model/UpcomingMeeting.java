package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;

/**
 * Domain model representing an upcoming meeting on the dashboard.
 *
 * <p>Contains a concise meeting-card payload for upcoming host meetings:
 * id, shortCode, title, startTime, endTime, status.
 */
public record UpcomingMeeting(
        String id,
        String shortCode,
        String title,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        MeetingStatus status) {}
