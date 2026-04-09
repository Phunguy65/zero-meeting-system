package io.github.phunguy65.zms.notification.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.zms.notification.infrastructure.config.NotificationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MeetingInvitationLinkFactoryTest {

    private MeetingInvitationLinkFactory linkFactory;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        properties.getResend().setApiKey("re_test_key");
        properties.getResend().setFromEmail("notifications@example.com");
        properties.getResend().setFromName("Zero Meeting System");
        properties.getInvitation().setJoinBaseUrl("https://app.example.com/join");
        properties.afterPropertiesSet();
        linkFactory = new MeetingInvitationLinkFactory(properties);
    }

    @Test
    void buildsUnprotectedJoinLinkWithoutPassword() {
        assertThat(linkFactory.buildJoinLink("ABC1234567", null))
                .isEqualTo("https://app.example.com/join?code=ABC1234567");
    }

    @Test
    void buildsProtectedJoinLinkWithPassword() {
        assertThat(linkFactory.buildJoinLink("ABC1234567", "secret123"))
                .isEqualTo("https://app.example.com/join?code=ABC1234567&password=secret123");
    }

    @Test
    void encodesPasswordCharactersSafely() {
        assertThat(linkFactory.buildJoinLink("ABC1234567", "pass word&more"))
                .isEqualTo(
                        "https://app.example.com/join?code=ABC1234567&password=pass%20word%26more");
    }

    @Test
    void omitsBlankPasswordValues() {
        assertThat(linkFactory.buildJoinLink("ABC1234567", "   "))
                .isEqualTo("https://app.example.com/join?code=ABC1234567");
    }
}
