package io.github.phunguy65.zms.meetingmanagement.domain.event;

import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Published when a meeting is scheduled with a non-empty invitee list.
 * Carries enough information for the notification service to send invitation emails.
 */
public record MeetingInvitationsSentEvent(
        UUID eventId,
        UUID aggregateId,
        String meetingTitle,
        @Nullable Instant startTime,
        List<InviteeInfo> invitees,
        Instant occurredAt)
        implements PublishableEvent {

    /**
     * Minimal invitee info needed by the notification service.
     *
     * @param userId      resolved user ID (may be null if resolution was partial — should not
     *                    happen in practice since we fail-fast on unresolved invitees)
     * @param email       the invite target email
     * @param displayName the user's full name at invite time
     */
    public record InviteeInfo(
            @Nullable UUID userId, String email, @Nullable String displayName) {}

    @Override
    public String aggregateType() {
        return "meeting";
    }

    @Override
    public String eventType() {
        return "io.github.phunguy65.zms.meeting.invitations-sent.v1";
    }

    @Override
    public String topic() {
        return "meeting-management.meeting.invitations-sent";
    }
}
