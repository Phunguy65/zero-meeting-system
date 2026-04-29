package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Domain model representing full meeting details for display and editing.
 *
 * <p>Includes all meeting metadata and settings required for pre-meeting edit mode
 * and in-meeting settings management.
 */
public record MeetingDetail(
        String id,
        @Nullable String hostId,
        String shortCode,
        @Nullable String title,
        OffsetDateTime startTime,
        @Nullable OffsetDateTime endTime,
        MeetingStatus status,
        MeetingType type,
        MeetingSettings settings) {}
