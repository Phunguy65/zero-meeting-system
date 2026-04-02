package io.github.phunguy65.zms.notification.infrastructure.email;

import io.github.phunguy65.zms.notification.infrastructure.config.NotificationProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class MeetingInvitationLinkFactory {

    private final NotificationProperties notificationProperties;

    public MeetingInvitationLinkFactory(NotificationProperties notificationProperties) {
        this.notificationProperties = notificationProperties;
    }

    public String buildJoinLink(String shortCode, @Nullable String rawPassword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                        notificationProperties.getInvitation().getJoinBaseUrl())
                .queryParam("code", shortCode);
        if (rawPassword != null && !rawPassword.isBlank()) {
            builder.queryParam("password", rawPassword);
        }
        return builder.build().encode().toUriString();
    }
}
