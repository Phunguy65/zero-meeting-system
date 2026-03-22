package io.github.phunguy65.zms.meetingmanagement.domain.event;

import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import java.time.Instant;
import java.util.UUID;

/** Published when a host approves a join request. */
public record JoinRequestApprovedEvent(
        UUID eventId,
        UUID meetingId,
        UUID joinRequestId,
        UUID approvedBy,
        String liveKitToken,
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
        return "io.github.phunguy65.zms.meeting.join_request_approved.v1";
    }

    @Override
    public String topic() {
        return "meeting-management.join_request.approved";
    }
}
