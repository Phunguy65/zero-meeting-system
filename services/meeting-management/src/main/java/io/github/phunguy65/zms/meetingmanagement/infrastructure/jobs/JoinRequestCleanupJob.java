package io.github.phunguy65.zms.meetingmanagement.infrastructure.jobs;

import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestExpiredEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job that expires join requests past their TTL.
 *
 * <p>Runs every 60 seconds, scans all {@code join_request:*} sorted sets for entries with score
 * (expiresAt) less than current time, marks them as {@code EXPIRED}, publishes
 * {@link JoinRequestExpiredEvent} via the Outbox pattern, and removes from queue.
 *
 * <p>Annotated with {@code @Transactional} to create the required transaction context for
 * {@code OutboxEventListener} ({@code @TransactionalEventListener(AFTER_COMMIT)}) to capture
 * domain events published within this method.
 */
@Component
public class JoinRequestCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(JoinRequestCleanupJob.class);

    private final StringRedisTemplate redisTemplate;
    private final JoinRequestRepository joinRequestRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public JoinRequestCleanupJob(
            StringRedisTemplate redisTemplate,
            JoinRequestRepository joinRequestRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.redisTemplate = redisTemplate;
        this.joinRequestRepository = joinRequestRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpiredRequests() {
        long now = Instant.now().toEpochMilli();

        Set<String> queueKeys = redisTemplate.keys("join_request:*");
        if (queueKeys == null || queueKeys.isEmpty()) {
            return;
        }

        int expiredCount = 0;
        for (String queueKey : queueKeys) {
            if (queueKey.contains("_meta:") || queueKey.contains("_device:")) {
                continue;
            }

            String meetingIdStr = queueKey.substring("join_request:".length());
            UUID meetingId;
            try {
                meetingId = UUID.fromString(meetingIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid meetingId in queue key: {}", queueKey);
                continue;
            }

            Set<String> expiredRequestIds =
                    redisTemplate.opsForZSet().rangeByScore(queueKey, 0, now);
            if (expiredRequestIds == null || expiredRequestIds.isEmpty()) {
                continue;
            }

            for (String requestIdStr : expiredRequestIds) {
                UUID requestId = UUID.fromString(requestIdStr);

                joinRequestRepository.updateStatus(requestId, JoinRequestStatus.EXPIRED);

                var expiredEvent = new JoinRequestExpiredEvent(
                        UUID.randomUUID(), meetingId, requestId, Instant.now());
                applicationEventPublisher.publishEvent(expiredEvent);

                joinRequestRepository.removeFromQueue(meetingId, requestId);

                expiredCount++;
            }
        }

        if (expiredCount > 0) {
            log.info("Expired {} join requests", expiredCount);
        }
    }
}
