package io.github.phunguy65.zms.meetingmanagement.domain.event;

import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Published when a participant successfully joins a LiveKit room (webhook: {@code participant_joined}).
 * Consumed by chat-management to create a system chat message.
 */
public record ParticipantJoinedEvent(
        UUID eventId, UUID meetingId, @Nullable UUID userId, String displayName, Instant occurredAt)
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
        return "io.github.phunguy65.zms.meeting.participant_joined.v1";
    }

    @Override
    public String topic() {
        return "meeting-management.participant.joined";
    }
}
