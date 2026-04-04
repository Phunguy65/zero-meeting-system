package io.github.phunguy65.zms.meetingmanagement.domain.event;

import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a meeting's settings are updated by the host.
 */
public record MeetingSettingsUpdatedEvent(
        UUID eventId,
        UUID aggregateId,
        UUID hostId,
        UUID updatedBy,
        MeetingStatus meetingStatus,
        Instant updatedAt)
        implements PublishableEvent {

    @Override
    public String aggregateType() {
        return "meeting";
    }

    @Override
    public String eventType() {
        return "io.github.phunguy65.zms.meeting.settings.updated.v1";
    }

    @Override
    public String topic() {
        return "meeting-management.meeting.settings.updated";
    }

    @Override
    public Instant occurredAt() {
        return updatedAt;
    }
}
