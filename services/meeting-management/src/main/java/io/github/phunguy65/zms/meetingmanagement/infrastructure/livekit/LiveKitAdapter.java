package io.github.phunguy65.zms.meetingmanagement.infrastructure.livekit;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * LiveKit adapter — generates JWT access tokens and manages rooms via the LiveKit HTTP API.
 *
 * <p>Token format follows the LiveKit JWT spec:
 * <ul>
 *   <li>Signed with HMAC-SHA256 using the API secret</li>
 *   <li>Claims: {@code iss} (API key), {@code sub} (participant identity), {@code video} grants</li>
 * </ul>
 */
@Component
public class LiveKitAdapter implements LiveKitPort {

    private static final Logger log = LoggerFactory.getLogger(LiveKitAdapter.class);

    private final String apiKey;
    private final SecretKey signingKey;
    private final long tokenExpirySeconds;
    private final RestClient restClient;

    public LiveKitAdapter(
            @Value("${app.livekit.api-key}") String apiKey,
            @Value("${app.livekit.api-secret}") String apiSecret,
            @Value("${app.livekit.token-expiry-seconds:7200}") long tokenExpirySeconds,
            @Value("${app.livekit.url}") String livekitUrl) {
        this.apiKey = apiKey;
        this.signingKey = Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));
        this.tokenExpirySeconds = tokenExpirySeconds;
        this.restClient = RestClient.builder().baseUrl(livekitUrl).build();
    }

    @Override
    public String generateToken(
            LiveKitRoomName roomName,
            @Nullable UUID userId,
            String displayName,
            ParticipantRole role) {
        String identity = userId != null ? userId.toString() : "guest-" + UUID.randomUUID();
        boolean canPublish = role == ParticipantRole.HOST;

        Map<String, Object> videoGrants = new HashMap<>();
        videoGrants.put("room", roomName.value());
        videoGrants.put("roomJoin", true);
        videoGrants.put("canPublish", canPublish);
        videoGrants.put("canPublishData", canPublish);
        videoGrants.put("canSubscribe", true);

        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(apiKey)
                .subject(identity)
                .claim("name", displayName)
                .claim("video", videoGrants)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(tokenExpirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public void createRoom(LiveKitRoomName roomName) {
        // LiveKit auto-creates rooms on first participant join.
        // Explicit creation is a no-op here; can be extended to pre-configure room options
        // via the LiveKit Twirp API if needed.
        log.debug("LiveKit room will be auto-created on join: {}", roomName.value());
    }

    @Override
    public void deleteRoom(LiveKitRoomName roomName) {
        // POST /twirp/livekit.RoomService/DeleteRoom
        // Requires a server-side admin token — omitted for now, can be added when needed.
        log.info("LiveKit room deletion requested: {}", roomName.value());
    }
}
