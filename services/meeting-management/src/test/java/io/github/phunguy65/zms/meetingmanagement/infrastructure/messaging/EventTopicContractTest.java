package io.github.phunguy65.zms.meetingmanagement.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import io.cloudevents.CloudEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestApprovedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestCreatedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestDeniedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestExpiredEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.ParticipantKickedEvent;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.sse.MeetingSseManager;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;

class EventTopicContractTest {

    @Test
    void meetingSseManagerListenerTopicsMatchJoinRequestAndParticipantEvents() throws Exception {
        assertThat(listenerTopics(MeetingSseManager.class, "onJoinRequestCreated"))
                .containsExactly(new JoinRequestCreatedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Guest",
                                "device-1",
                                Instant.parse("2026-04-02T09:15:00Z"))
                        .topic());
        assertThat(listenerTopics(MeetingSseManager.class, "onJoinRequestApproved"))
                .containsExactly(new JoinRequestApprovedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "livekit-token",
                                Instant.parse("2026-04-02T09:20:00Z"))
                        .topic());
        assertThat(listenerTopics(MeetingSseManager.class, "onJoinRequestDenied"))
                .containsExactly(new JoinRequestDeniedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                Instant.parse("2026-04-02T09:25:00Z"))
                        .topic());
        assertThat(listenerTopics(MeetingSseManager.class, "onJoinRequestExpired"))
                .containsExactly(new JoinRequestExpiredEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                Instant.parse("2026-04-02T09:30:00Z"))
                        .topic());
        assertThat(listenerTopics(MeetingSseManager.class, "onParticipantKicked"))
                .containsExactly(new ParticipantKickedEvent(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                "Charlie",
                                Instant.parse("2026-04-02T09:10:00Z"))
                        .topic());
    }

    private static String[] listenerTopics(Class<?> owner, String methodName) throws Exception {
        Method method = owner.getMethod(methodName, CloudEvent.class);
        KafkaListener annotation = method.getAnnotation(KafkaListener.class);
        assertThat(annotation).isNotNull();
        return annotation.topics();
    }
}
