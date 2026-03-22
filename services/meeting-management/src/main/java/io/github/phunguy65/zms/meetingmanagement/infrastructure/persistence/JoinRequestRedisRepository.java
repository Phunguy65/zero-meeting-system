package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.JoinRequestId;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed repository for join requests.
 *
 * <p>Data structures:
 * <ul>
 *   <li>{@code ZSET join_request:{meetingId}} — queue ordered by expiresAt (score)</li>
 *   <li>{@code HASH join_request_meta:{requestId}} — request metadata</li>
 *   <li>{@code STRING join_request_device:{meetingId}:{deviceId}} — duplicate detection index</li>
 * </ul>
 */
@Repository
public class JoinRequestRedisRepository implements JoinRequestRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JoinRequestRedisRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(JoinRequest request, Duration ttl) {
        String meetingId = request.getMeetingId().value().toString();
        String requestId = request.getId().value().toString();
        String deviceId = request.getDeviceId();

        // 1. Add to sorted set (queue)
        String queueKey = queueKey(meetingId);
        double score = request.getExpiresAt().toEpochMilli();
        redisTemplate.opsForZSet().add(queueKey, requestId, score);

        // 2. Store metadata as hash
        String metaKey = metaKey(requestId);
        Map<String, String> fields = new HashMap<>();
        fields.put("id", requestId);
        fields.put("meetingId", meetingId);
        fields.put("userId", request.getUserId().map(u -> u.value().toString()).orElse(null));
        fields.put("displayName", request.getDisplayName());
        fields.put("deviceId", deviceId);
        fields.put("status", request.getStatus().name());
        fields.put("requestedAt", request.getRequestedAt().toString());
        fields.put("expiresAt", request.getExpiresAt().toString());

        redisTemplate.opsForHash().putAll(metaKey, fields);
        redisTemplate.expire(metaKey, ttl.toSeconds(), TimeUnit.SECONDS);

        // 3. Create device index for duplicate detection
        String deviceKey = deviceKey(meetingId, deviceId);
        redisTemplate.opsForValue().set(deviceKey, requestId, ttl.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    public Optional<JoinRequest> findById(UUID requestId) {
        String metaKey = metaKey(requestId.toString());
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(metaKey);
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToJoinRequest(fields));
    }

    @Override
    public Optional<JoinRequest> findByDeviceId(UUID meetingId, String deviceId) {
        String deviceKey = deviceKey(meetingId.toString(), deviceId);
        String requestId = redisTemplate.opsForValue().get(deviceKey);
        if (requestId == null) {
            return Optional.empty();
        }
        return findById(UUID.fromString(requestId));
    }

    @Override
    public List<JoinRequest> findPendingByMeetingId(UUID meetingId) {
        String queueKey = queueKey(meetingId.toString());
        
        // Get all request IDs from sorted set
        Set<String> requestIds = redisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (requestIds == null || requestIds.isEmpty()) {
            return List.of();
        }

        // Batch fetch metadata for all requests
        List<JoinRequest> results = new ArrayList<>();
        for (String requestId : requestIds) {
            String metaKey = metaKey(requestId);
            Map<Object, Object> fields = redisTemplate.opsForHash().entries(metaKey);
            if (!fields.isEmpty()) {
                JoinRequest request = mapToJoinRequest(fields);
                if (request.getStatus() == JoinRequestStatus.PENDING) {
                    results.add(request);
                }
            }
        }

        // Sort by requestedAt ascending
        results.sort(Comparator.comparing(JoinRequest::getRequestedAt));
        return results;
    }

    @Override
    public void updateStatus(UUID requestId, JoinRequestStatus status) {
        String metaKey = metaKey(requestId.toString());
        redisTemplate.opsForHash().put(metaKey, "status", status.name());
    }

    @Override
    public void removeFromQueue(UUID meetingId, UUID requestId) {
        String queueKey = queueKey(meetingId.toString());
        String metaKey = metaKey(requestId.toString());

        // Get deviceId before deleting metadata
        Object deviceIdObj = redisTemplate.opsForHash().get(metaKey, "deviceId");
        String deviceId = deviceIdObj != null ? deviceIdObj.toString() : null;

        // Remove from queue
        redisTemplate.opsForZSet().remove(queueKey, requestId.toString());

        // Delete metadata
        redisTemplate.delete(metaKey);

        // Delete device index
        if (deviceId != null) {
            String deviceKey = deviceKey(meetingId.toString(), deviceId);
            redisTemplate.delete(deviceKey);
        }
    }

    @Override
    public void deleteAllByMeetingId(UUID meetingId) {
        String queueKey = queueKey(meetingId.toString());

        // Get all request IDs
        Set<String> requestIds = redisTemplate.opsForZSet().range(queueKey, 0, -1);
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }

        // Delete all metadata and device indexes
        for (String requestId : requestIds) {
            String metaKey = metaKey(requestId);
            
            // Get deviceId before deleting
            Object deviceIdObj = redisTemplate.opsForHash().get(metaKey, "deviceId");
            String deviceId = deviceIdObj != null ? deviceIdObj.toString() : null;

            // Delete metadata
            redisTemplate.delete(metaKey);

            // Delete device index
            if (deviceId != null) {
                String deviceKey = deviceKey(meetingId.toString(), deviceId);
                redisTemplate.delete(deviceKey);
            }
        }

        // Delete queue
        redisTemplate.delete(queueKey);
    }

    private JoinRequest mapToJoinRequest(Map<Object, Object> fields) {
        String id = (String) fields.get("id");
        String meetingId = (String) fields.get("meetingId");
        String userIdStr = (String) fields.get("userId");
        String displayName = (String) fields.get("displayName");
        String deviceId = (String) fields.get("deviceId");
        String statusStr = (String) fields.get("status");
        String requestedAtStr = (String) fields.get("requestedAt");
        String expiresAtStr = (String) fields.get("expiresAt");

        return JoinRequest.reconstitute(
                JoinRequestId.of(UUID.fromString(id)),
                MeetingId.of(UUID.fromString(meetingId)),
                userIdStr != null ? UserId.of(UUID.fromString(userIdStr)) : null,
                displayName,
                deviceId,
                JoinRequestStatus.valueOf(statusStr),
                Instant.parse(requestedAtStr),
                Instant.parse(expiresAtStr));
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
}
