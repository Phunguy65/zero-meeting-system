package io.github.phunguy65.zms.meetingmanagement.domain.event;

import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import java.time.Instant;
import java.util.UUID;

/** Published when a meeting transitions SCHEDULED → CANCELLED. */
public record MeetingCancelledEvent(
        UUID eventId, UUID aggregateId, UUID hostId, Instant cancelledAt)
        implements PublishableEvent {

    @Override
    public String aggregateType() {
        return "meeting";
    }

    @Override
    public String eventType() {
        return "io.github.phunguy65.zms.meeting.cancelled.v1";
    }

    @Override
    public String topic() {
        return "meeting-management.meeting.cancelled";
    }

    @Override
    public Instant occurredAt() {
        return cancelledAt;
    }
}
