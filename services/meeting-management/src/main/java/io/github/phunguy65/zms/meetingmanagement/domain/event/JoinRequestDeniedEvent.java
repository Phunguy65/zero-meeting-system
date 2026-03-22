package io.github.phunguy65.zms.meetingmanagement.domain.event;

import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/** Published when a host denies a join request or when a meeting ends with pending requests. */
public record JoinRequestDeniedEvent(
        UUID eventId,
        UUID meetingId,
        UUID joinRequestId,
        @Nullable UUID deniedBy,
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
        return "io.github.phunguy65.zms.meeting.join_request_denied.v1";
    }

    @Override
    public String topic() {
        return "meeting-management.join_request.denied";
    }
}
