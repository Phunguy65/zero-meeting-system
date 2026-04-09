package io.github.phunguy65.zms.notification.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.zms.notification.infrastructure.messaging.MeetingInvitationsSentMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeetingInvitationEmailRendererTest {

    private final MeetingInvitationEmailRenderer renderer = new MeetingInvitationEmailRenderer();

    @Test
    void rendersEmailWithRequiredMetadata() {
        var rendered = renderer.render(
                invitation("Planning Session", Instant.parse("2026-04-03T10:00:00Z"), null),
                invitee("Alice"),
                "https://app.example.com/join?code=ABC1234567");

        assertThat(rendered.subject()).isEqualTo("Invitation: Planning Session");
        assertThat(rendered.html())
                .contains("Hello Alice")
                .contains("You're invited to Planning Session")
                .contains("Fri, 03 Apr 2026 at 10:00 UTC")
                .contains("https://app.example.com/join?code=ABC1234567");
    }

    @Test
    void usesFallbackCopyForMissingOptionalFields() {
        var rendered = renderer.render(
                invitation(null, null, null),
                invitee(null),
                "https://app.example.com/join?code=ABC1234567");

        assertThat(rendered.subject()).isEqualTo("Invitation: your Zero Meeting System meeting");
        assertThat(rendered.html())
                .contains("Hello there")
                .contains("your Zero Meeting System meeting")
                .contains("The host will start the meeting soon.");
    }

    @Test
    void usesFallbackCopyForBlankOptionalFields() {
        var rendered = renderer.render(
                invitation("   ", null, null),
                invitee("   "),
                "https://app.example.com/join?code=ABC1234567");

        assertThat(rendered.subject()).isEqualTo("Invitation: your Zero Meeting System meeting");
        assertThat(rendered.html())
                .contains("Hello there")
                .contains("your Zero Meeting System meeting");
    }

    @Test
    void escapesUserControlledFields() {
        var rendered = renderer.render(
                invitation("<script>alert(1)</script>", null, null),
                invitee("<b>Alice</b>"),
                "https://app.example.com/join?code=<unsafe>&password=abc");

        assertThat(rendered.html())
                .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
                .contains("&lt;b&gt;Alice&lt;/b&gt;")
                .contains("code=&lt;unsafe&gt;&amp;password=abc")
                .doesNotContain("<script>alert(1)</script>")
                .doesNotContain("<b>Alice</b>");
    }

    @Test
    void stripsLineBreaksFromSubject() {
        var rendered = renderer.render(
                invitation("Planning\nSession\rTitle", null, null),
                invitee("Alice"),
                "https://app.example.com/join?code=ABC1234567");

        assertThat(rendered.subject())
                .isEqualTo("Invitation: Planning Session Title")
                .doesNotContain("\n")
                .doesNotContain("\r");
    }

    private MeetingInvitationsSentMessage invitation(
            String title, Instant startTime, String rawPassword) {
        return new MeetingInvitationsSentMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                title,
                "ABC1234567",
                startTime,
                rawPassword,
                List.of(invitee("Alice")),
                Instant.parse("2026-04-02T09:00:00Z"));
    }

    private MeetingInvitationsSentMessage.InviteeInfo invitee(String displayName) {
        return new MeetingInvitationsSentMessage.InviteeInfo(
                UUID.randomUUID(), "alice@example.com", displayName);
    }
}
