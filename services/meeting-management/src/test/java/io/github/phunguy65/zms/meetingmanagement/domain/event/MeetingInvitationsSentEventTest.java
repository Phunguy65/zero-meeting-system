package io.github.phunguy65.zms.meetingmanagement.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeetingInvitationsSentEventTest {

    @Test
    void toStringRedactsRawPasswordWhenPresent() {
        MeetingInvitationsSentEvent event = new MeetingInvitationsSentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Planning Session",
                "ABC1234567",
                Instant.parse("2026-04-02T10:15:30Z"),
                "s3cr3t!",
                List.of(new MeetingInvitationsSentEvent.InviteeInfo(
                        UUID.randomUUID(), "alice@example.com", "Alice")),
                Instant.parse("2026-04-02T09:00:00Z"));

        assertThat(event.toString())
                .contains("meetingShortCode=ABC1234567")
                .contains("rawPassword=<redacted:true>")
                .doesNotContain("s3cr3t!");
    }

    @Test
    void toStringHandlesNullableFieldsWithoutExposingPassword() {
        MeetingInvitationsSentEvent event = new MeetingInvitationsSentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "XYZ7654321",
                null,
                null,
                List.of(new MeetingInvitationsSentEvent.InviteeInfo(
                        null, "guest@example.com", null)),
                Instant.parse("2026-04-02T09:00:00Z"));

        assertThat(event.toString())
                .contains("meetingTitle=null")
                .contains("startTime=null")
                .contains("rawPassword=<redacted:false>");
    }
}
