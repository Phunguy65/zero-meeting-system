package io.github.phunguy65.zms.notification.application.usecase;

import io.github.phunguy65.zms.notification.domain.port.EmailSender;
import io.github.phunguy65.zms.notification.infrastructure.email.MeetingCancelledEmailRenderer;
import io.github.phunguy65.zms.notification.infrastructure.messaging.MeetingCancelledMessage;
import org.springframework.stereotype.Service;

@Service
public class SendMeetingCancelledEmailUseCase {

    private final MeetingCancelledEmailRenderer renderer;
    private final EmailSender emailSender;

    public SendMeetingCancelledEmailUseCase(
            MeetingCancelledEmailRenderer renderer, EmailSender emailSender) {
        this.renderer = renderer;
        this.emailSender = emailSender;
    }

    public void send(
            MeetingCancelledMessage cancellation, MeetingCancelledMessage.InviteeInfo invitee) {
        MeetingCancelledEmailRenderer.RenderedEmail renderedEmail =
                renderer.render(cancellation, invitee);
        emailSender.send(invitee.email(), renderedEmail.subject(), renderedEmail.html());
    }
}
