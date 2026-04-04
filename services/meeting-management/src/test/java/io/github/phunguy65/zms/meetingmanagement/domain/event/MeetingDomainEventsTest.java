package io.github.phunguy65.zms.meetingmanagement.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeetingDomainEventsTest {

    @Test
    void meetingSettingsUpdatedEvent_exposesExpectedMetadata() {
        var event = new MeetingSettingsUpdatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                MeetingStatus.SCHEDULED,
                Instant.parse("2026-04-02T09:00:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.settings.updated.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.meeting.settings.updated");
    }

    @Test
    void participantJoinedEvent_exposesExpectedMetadata() {
        var event = new ParticipantJoinedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Alice",
                Instant.parse("2026-04-02T09:00:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.participant.joined.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.participant.joined");
    }

    @Test
    void participantLeftEvent_exposesExpectedMetadata() {
        var event = new ParticipantLeftEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Bob",
                Instant.parse("2026-04-02T09:05:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.participant.left.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.participant.left");
    }

    @Test
    void participantKickedEvent_exposesExpectedMetadata() {
        var event = new ParticipantKickedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Charlie",
                Instant.parse("2026-04-02T09:10:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.participant.kicked.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.participant.kicked");
    }

    @Test
    void joinRequestCreatedEvent_exposesExpectedMetadata() {
        var event = new JoinRequestCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Guest",
                "device-1",
                Instant.parse("2026-04-02T09:15:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.join-request.created.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.join-request.created");
    }

    @Test
    void joinRequestApprovedEvent_exposesExpectedMetadata() {
        var event = new JoinRequestApprovedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "livekit-token",
                Instant.parse("2026-04-02T09:20:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.join-request.approved.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.join-request.approved");
    }

    @Test
    void joinRequestDeniedEvent_exposesExpectedMetadata() {
        var event = new JoinRequestDeniedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-04-02T09:25:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.join-request.denied.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.join-request.denied");
    }

    @Test
    void joinRequestExpiredEvent_exposesExpectedMetadata() {
        var event = new JoinRequestExpiredEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-04-02T09:30:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.join-request.expired.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.join-request.expired");
    }

    @Test
    void meetingInvitationsSentEvent_exposesExpectedMetadata() {
        var event = new MeetingInvitationsSentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Planning Session",
                "ABC1234567",
                Instant.parse("2026-04-02T10:15:30Z"),
                "secret",
                List.of(new MeetingInvitationsSentEvent.InviteeInfo(
                        UUID.randomUUID(), "alice@example.com", "Alice")),
                Instant.parse("2026-04-02T09:00:00Z"));

        assertThat(event.aggregateType()).isEqualTo("meeting");
        assertThat(event.eventType())
                .isEqualTo("io.github.phunguy65.zms.meeting.invitations.sent.v1");
        assertThat(event.topic()).isEqualTo("meeting-management.meeting.invitations.sent");
    }
}
