package io.github.phunguy65.zms.meetingmanagement.infrastructure.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Publishes SSE events to Redis Pub/Sub for multi-instance broadcasting.
 *
 * <p>Events are published to {@code meeting:{meetingId}:events} channel and
 * picked up by {@link MeetingSseManager} on all backend instances.
 */
@Component
public class RedisSseEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisSseEventPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisSseEventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes an SSE event to Redis Pub/Sub.
     *
     * @param meetingId the meeting ID
     * @param eventType the event type (e.g., "join_request_created")
     * @param data the event data (will be serialized to JSON)
     */
    public void publish(UUID meetingId, String eventType, Object data) {
        String channel = "meeting:" + meetingId + ":events";
        SseEvent event = new SseEvent(eventType, data);

        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, payload);
            log.debug("Published SSE event {} to channel {}", eventType, channel);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize SSE event {}: {}", eventType, e.getMessage(), e);
        }
    }
}
