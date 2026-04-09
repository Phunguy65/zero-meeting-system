package io.github.phunguy65.zms.notification.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.zms.notification.domain.port.EmailSender;
import io.github.phunguy65.zms.notification.infrastructure.email.MeetingInvitationEmailRenderer;
import io.github.phunguy65.zms.notification.infrastructure.email.MeetingInvitationLinkFactory;
import io.github.phunguy65.zms.notification.infrastructure.messaging.MeetingInvitationsSentMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendMeetingInvitationEmailUseCaseTest {

    @Mock
    private MeetingInvitationLinkFactory linkFactory;

    @Mock
    private MeetingInvitationEmailRenderer renderer;

    @Mock
    private EmailSender emailSender;

    private SendMeetingInvitationEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SendMeetingInvitationEmailUseCase(linkFactory, renderer, emailSender);
    }

    @Test
    void orchestratesLinkRenderingAndSending() {
        MeetingInvitationsSentMessage invitation = new MeetingInvitationsSentMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Planning Session",
                "ABC1234567",
                Instant.parse("2026-04-03T10:00:00Z"),
                "secret123",
                List.of(new MeetingInvitationsSentMessage.InviteeInfo(
                        UUID.randomUUID(), "alice@example.com", "Alice")),
                Instant.parse("2026-04-02T09:00:00Z"));
        MeetingInvitationsSentMessage.InviteeInfo invitee =
                invitation.invitees().getFirst();

        when(linkFactory.buildJoinLink("ABC1234567", "secret123"))
                .thenReturn("https://app.example.com/join?code=ABC1234567&password=secret123");
        when(renderer.render(
                        invitation,
                        invitee,
                        "https://app.example.com/join?code=ABC1234567&password=secret123"))
                .thenReturn(new MeetingInvitationEmailRenderer.RenderedEmail(
                        "Invitation", "<p>Hello</p>"));

        useCase.send(invitation, invitee);

        verify(linkFactory).buildJoinLink("ABC1234567", "secret123");
        verify(renderer)
                .render(
                        invitation,
                        invitee,
                        "https://app.example.com/join?code=ABC1234567&password=secret123");
        verify(emailSender).send("alice@example.com", "Invitation", "<p>Hello</p>");
    }
}
