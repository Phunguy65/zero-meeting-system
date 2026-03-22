package io.github.phunguy65.zms.meetingmanagement.domain.event;

import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/** Published when a participant submits a join request for a meeting with MANUAL_APPROVAL policy. */
public record JoinRequestCreatedEvent(
        UUID eventId,
        UUID meetingId,
        UUID joinRequestId,
        @Nullable UUID userId,
        String displayName,
        String deviceId,
        Instant occurredAt)
        implements PublishableEvent {

    @Override
    public UUID aggregateId() {
        return meetingId;
    }

    @Override
    public String aggregateType() {
        return "meeting";
    }

    @Override
    public String eventType() {
        return "io.github.phunguy65.zms.meeting.join_request_created.v1";
    }

    @Override
    public String topic() {
        return "meeting-management.join_request.created";
    }
}
