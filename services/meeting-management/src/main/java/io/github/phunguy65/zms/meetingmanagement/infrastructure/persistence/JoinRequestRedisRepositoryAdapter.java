package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.JoinRequestSummary;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence.model.JoinRequestData;
import io.github.phunguy65.zms.shared.domain.OffsetPageResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis-backed repository for join requests.
 *
 * <p>Data structures:
 * <ul>
 *   <li>{@code ZSET join_request:{meetingId}} — queue ordered by expiresAt (score)</li>
 *   <li>{@code STRING join_request_meta:{requestId}} — request metadata (JSON)</li>
 *   <li>{@code STRING join_request_device:{meetingId}:{deviceId}} — duplicate detection index</li>
 * </ul>
 */
@Repository
public class JoinRequestRedisRepositoryAdapter implements JoinRequestRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, JoinRequestData> joinRequestRedisTemplate;

    public JoinRequestRedisRepositoryAdapter(
            StringRedisTemplate stringRedisTemplate,
            RedisTemplate<String, JoinRequestData> joinRequestRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.joinRequestRedisTemplate = joinRequestRedisTemplate;
    }

    @Override
    public void save(JoinRequest request, Duration ttl) {
        String meetingId = request.getMeetingId().value().toString();
        String requestId = request.getId().value().toString();
        String deviceId = request.getDeviceId();

        String queueKey = queueKey(meetingId);
        double score = request.getExpiresAt().toEpochMilli();
        stringRedisTemplate.opsForZSet().add(queueKey, requestId, score);

        String metaKey = metaKey(requestId);
        JoinRequestData data = JoinRequestData.from(request);
        joinRequestRedisTemplate.opsForValue().set(metaKey, data, ttl);

        String deviceKey = deviceKey(meetingId, deviceId);
        stringRedisTemplate
                .opsForValue()
                .set(deviceKey, requestId, ttl.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public Optional<JoinRequest> findById(UUID requestId) {
        String metaKey = metaKey(requestId.toString());
        JoinRequestData data = joinRequestRedisTemplate.opsForValue().get(metaKey);
        return Optional.ofNullable(data).map(JoinRequestData::toDomain);
    }

    @Override
    public Optional<JoinRequest> findByDeviceId(UUID meetingId, String deviceId) {
        String deviceKey = deviceKey(meetingId.toString(), deviceId);
        String requestId = stringRedisTemplate.opsForValue().get(deviceKey);
        if (requestId == null) {
            return Optional.empty();
        }
        return findById(UUID.fromString(requestId));
    }

    @Override
    public List<JoinRequest> findPendingByMeetingId(UUID meetingId) {
        String queueKey = queueKey(meetingId.toString());

        Set<String> requestIds = stringRedisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (requestIds == null || requestIds.isEmpty()) {
            return List.of();
        }

        List<JoinRequest> results = new ArrayList<>();
        for (String requestId : requestIds) {
            String metaKey = metaKey(requestId);
            JoinRequestData data = joinRequestRedisTemplate.opsForValue().get(metaKey);
            if (data != null) {
                JoinRequest request = data.toDomain();
                if (request.getStatus() == JoinRequestStatus.PENDING) {
                    results.add(request);
                }
            }
        }

        results.sort(Comparator.comparing(JoinRequest::getRequestedAt));
        return results;
    }

    @Override
    public OffsetPageResponse<JoinRequestSummary> findPendingSummariesByMeetingId(
            UUID meetingId, int offset, int pageSize) {
        String queueKey = queueKey(meetingId.toString());

        Set<String> requestIds = stringRedisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (requestIds == null || requestIds.isEmpty()) {
            return OffsetPageResponse.empty(pageSize, offset);
        }

        List<JoinRequestSummary> allItems = new ArrayList<>();
        for (String requestId : requestIds) {
            JoinRequestData data = joinRequestRedisTemplate.opsForValue().get(metaKey(requestId));
            if (data != null
                    && JoinRequestStatus.valueOf(data.status()) == JoinRequestStatus.PENDING) {
                allItems.add(toSummary(data));
            }
        }

        allItems.sort(Comparator.comparing(JoinRequestSummary::requestedAt));
        if (offset >= allItems.size()) {
            return OffsetPageResponse.empty(pageSize, offset);
        }

        int endExclusive = Math.min(offset + pageSize, allItems.size());
        boolean hasNext = endExclusive < allItems.size();
        return OffsetPageResponse.of(
                allItems.subList(offset, endExclusive), pageSize, offset, hasNext);
    }

    @Override
    public void updateStatus(UUID requestId, JoinRequestStatus status) {
        String metaKey = metaKey(requestId.toString());

        JoinRequestData existingData = joinRequestRedisTemplate.opsForValue().get(metaKey);
        if (existingData == null) {
            return;
        }

        JoinRequestData updatedData = new JoinRequestData(
                existingData.id(),
                existingData.meetingId(),
                existingData.userId(),
                existingData.displayName(),
                existingData.deviceId(),
                status.name(),
                existingData.requestedAt(),
                existingData.expiresAt());

        Long ttl = joinRequestRedisTemplate.getExpire(metaKey, TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            joinRequestRedisTemplate
                    .opsForValue()
                    .set(metaKey, updatedData, Duration.ofSeconds(ttl));
        } else {
            joinRequestRedisTemplate.opsForValue().set(metaKey, updatedData);
        }
    }

    @Override
    public void removeFromQueue(UUID meetingId, UUID requestId) {
        String queueKey = queueKey(meetingId.toString());
        String metaKey = metaKey(requestId.toString());

        JoinRequestData data = joinRequestRedisTemplate.opsForValue().get(metaKey);
        String deviceId = data != null ? data.deviceId() : null;

        stringRedisTemplate.opsForZSet().remove(queueKey, requestId.toString());

        joinRequestRedisTemplate.delete(metaKey);

        if (deviceId != null) {
            String deviceKey = deviceKey(meetingId.toString(), deviceId);
            stringRedisTemplate.delete(deviceKey);
        }
    }

    @Override
    public void deleteAllByMeetingId(UUID meetingId) {
        String queueKey = queueKey(meetingId.toString());

        Set<String> requestIds = stringRedisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }

        for (String requestId : requestIds) {
            String metaKey = metaKey(requestId);

            JoinRequestData data = joinRequestRedisTemplate.opsForValue().get(metaKey);
            String deviceId = data != null ? data.deviceId() : null;

            joinRequestRedisTemplate.delete(metaKey);

            if (deviceId != null) {
                String deviceKey = deviceKey(meetingId.toString(), deviceId);
                stringRedisTemplate.delete(deviceKey);
            }
        }

        stringRedisTemplate.delete(queueKey);
    }

    private String queueKey(String meetingId) {
        return "join_request:" + meetingId;
    }

    private String metaKey(String requestId) {
        return "join_request_meta:" + requestId;
    }

    private String deviceKey(String meetingId, String deviceId) {
        return "join_request_device:" + meetingId + ":" + deviceId;
    }

    private JoinRequestSummary toSummary(JoinRequestData data) {
        return new JoinRequestSummary(
                UUID.fromString(data.id()),
                UUID.fromString(data.meetingId()),
                data.userId() != null ? UUID.fromString(data.userId()) : null,
                data.displayName(),
                JoinRequestStatus.valueOf(data.status()),
                java.time.Instant.parse(data.requestedAt()),
                java.time.Instant.parse(data.expiresAt()));
    }
}
