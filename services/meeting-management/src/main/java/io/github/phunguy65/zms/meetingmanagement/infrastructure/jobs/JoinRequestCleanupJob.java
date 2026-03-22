package io.github.phunguy65.zms.meetingmanagement.infrastructure.jobs;

import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.sse.RedisSseEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Scheduled job that expires join requests past their TTL.
 *
 * <p>Runs every 60 seconds, scans all {@code join_request:*} sorted sets for
 * entries with score (expiresAt) less than current time, marks them as {@code EXPIRED},
 * publishes {@code JoinRequestExpiredEvent} to Redis Pub/Sub, and removes from queue.
 */
@Component
public class JoinRequestCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(JoinRequestCleanupJob.class);

    private final StringRedisTemplate redisTemplate;
    private final JoinRequestRepository joinRequestRepository;
    private final RedisSseEventPublisher sseEventPublisher;

    public JoinRequestCleanupJob(
            StringRedisTemplate redisTemplate,
            JoinRequestRepository joinRequestRepository,
            RedisSseEventPublisher sseEventPublisher) {
        this.redisTemplate = redisTemplate;
        this.joinRequestRepository = joinRequestRepository;
        this.sseEventPublisher = sseEventPublisher;
    }

    @Scheduled(fixedDelay = 60_000) // Every 60 seconds
    public void cleanupExpiredRequests() {
        long now = Instant.now().toEpochMilli();

        // Find all join_request:* keys
        Set<String> queueKeys = redisTemplate.keys("join_request:*");
        if (queueKeys == null || queueKeys.isEmpty()) {
            return;
        }

        int expiredCount = 0;
        for (String queueKey : queueKeys) {
            // Skip metadata and device index keys
            if (queueKey.contains("_meta:") || queueKey.contains("_device:")) {
                continue;
            }

            // Extract meetingId from key: join_request:{meetingId}
            String meetingIdStr = queueKey.substring("join_request:".length());
            UUID meetingId;
            try {
                meetingId = UUID.fromString(meetingIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid meetingId in queue key: {}", queueKey);
                continue;
            }

            // Find expired entries (score < now)
            Set<String> expiredRequestIds =
                    redisTemplate.opsForZSet().rangeByScore(queueKey, 0, now);
            if (expiredRequestIds == null || expiredRequestIds.isEmpty()) {
                continue;
            }

            for (String requestIdStr : expiredRequestIds) {
                UUID requestId = UUID.fromString(requestIdStr);

                // Update status to EXPIRED
                joinRequestRepository.updateStatus(requestId, JoinRequestStatus.EXPIRED);

                // Publish expired event to SSE
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("requestId", requestId.toString());
                eventData.put("meetingId", meetingId.toString());
                eventData.put("status", "EXPIRED");
                sseEventPublisher.publish(meetingId, "join_request_expired", eventData);

                // Remove from queue
                joinRequestRepository.removeFromQueue(meetingId, requestId);

                expiredCount++;
            }
        }

        if (expiredCount > 0) {
            log.info("Expired {} join requests", expiredCount);
        }
    }
}
