package io.github.phunguy65.zms.notification.application.usecase;

import io.github.phunguy65.zms.notification.domain.port.EmailSender;
import io.github.phunguy65.zms.notification.infrastructure.email.InviteUpdatedEmailRenderer;
import io.github.phunguy65.zms.notification.infrastructure.messaging.MeetingInviteTokensInvalidatedMessage;
import org.springframework.stereotype.Service;

/**
 * Sends a "your invite link has been updated" email to an invitee whose token was invalidated
 * due to a host password change on a scheduled meeting.
 */
@Service
public class SendInviteUpdatedEmailUseCase {

    private final InviteUpdatedEmailRenderer renderer;
    private final EmailSender emailSender;

    public SendInviteUpdatedEmailUseCase(
            InviteUpdatedEmailRenderer renderer, EmailSender emailSender) {
        this.renderer = renderer;
        this.emailSender = emailSender;
    }

    public void send(
            MeetingInviteTokensInvalidatedMessage event,
            MeetingInviteTokensInvalidatedMessage.AffectedInviteeInfo invitee) {
        InviteUpdatedEmailRenderer.RenderedEmail renderedEmail = renderer.render(event, invitee);
        emailSender.send(invitee.email(), renderedEmail.subject(), renderedEmail.html());
    }
}
