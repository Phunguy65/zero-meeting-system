package io.github.phunguy65.zms.notification.application.usecase;

import io.github.phunguy65.zms.notification.domain.port.EmailSender;
import io.github.phunguy65.zms.notification.infrastructure.email.MeetingInvitationEmailRenderer;
import io.github.phunguy65.zms.notification.infrastructure.email.MeetingInvitationLinkFactory;
import io.github.phunguy65.zms.notification.infrastructure.messaging.MeetingInvitationsSentMessage;
import org.springframework.stereotype.Service;

@Service
public class SendMeetingInvitationEmailUseCase {

    private final MeetingInvitationLinkFactory meetingInvitationLinkFactory;
    private final MeetingInvitationEmailRenderer meetingInvitationEmailRenderer;
    private final EmailSender emailSender;

    public SendMeetingInvitationEmailUseCase(
            MeetingInvitationLinkFactory meetingInvitationLinkFactory,
            MeetingInvitationEmailRenderer meetingInvitationEmailRenderer,
            EmailSender emailSender) {
        this.meetingInvitationLinkFactory = meetingInvitationLinkFactory;
        this.meetingInvitationEmailRenderer = meetingInvitationEmailRenderer;
        this.emailSender = emailSender;
    }

    public void send(
            MeetingInvitationsSentMessage invitation,
            MeetingInvitationsSentMessage.InviteeInfo invitee) {
        String joinLink = meetingInvitationLinkFactory.buildJoinLink(
                invitation.meetingShortCode(), invitation.rawPassword());
        MeetingInvitationEmailRenderer.RenderedEmail renderedEmail =
                meetingInvitationEmailRenderer.render(invitation, invitee, joinLink);
        emailSender.send(invitee.email(), renderedEmail.subject(), renderedEmail.html());
    }
}
