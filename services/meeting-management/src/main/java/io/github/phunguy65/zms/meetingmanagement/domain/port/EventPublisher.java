package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;

/**
 * Port for publishing domain events to the external message broker (Kafka).
 */
public interface EventPublisher {

    void publish(PublishableEvent event);
}
