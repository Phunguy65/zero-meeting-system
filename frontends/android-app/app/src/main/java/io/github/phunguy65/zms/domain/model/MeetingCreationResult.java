package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;

/**
 * Domain model representing the result of creating a meeting (instant or scheduled).
 * Contains the essential fields needed by presentation layer after meeting creation.
 */
public class MeetingCreationResult {

    private final String meetingId;
    private final String shortCode;
    private final String title;
    private final MeetingType type;
    private final MeetingStatus status;
    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final OffsetDateTime createdAt;

    public MeetingCreationResult(
            String meetingId,
            String shortCode,
            String title,
            MeetingType type,
            MeetingStatus status,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            OffsetDateTime createdAt) {
        this.meetingId = meetingId;
        this.shortCode = shortCode;
        this.title = title;
        this.type = type;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getTitle() {
        return title;
    }

    public MeetingType getType() {
        return type;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
