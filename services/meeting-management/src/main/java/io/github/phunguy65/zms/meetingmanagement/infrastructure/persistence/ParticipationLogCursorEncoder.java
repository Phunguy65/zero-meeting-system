package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogCursor;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogId;
import io.github.phunguy65.zms.shared.domain.CursorErrorCode;
import io.github.phunguy65.zms.shared.domain.Result;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 cursor encoder for participation log keyset pagination.
 *
 * <p>Token format: Base64url( "{joinedAt_epoch_ms}:{id}:{hmac_prefix}" )
 * Uses {@link ParticipationLogId} (bigserial Long) instead of UUID.
 */
@Component
public class ParticipationLogCursorEncoder {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int HMAC_BYTES = 16;

    private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();

    private final byte[] secretBytes;

    public ParticipationLogCursorEncoder(@Value("${app.cursor.secret}") String secret) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String encode(Instant joinedAt, ParticipationLogId id) {
        String payload = joinedAt.toEpochMilli() + ":" + id.value();
        String signature = hmacPrefix(payload);
        String raw = payload + ":" + signature;
        return BASE64_ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Result<ParticipationLogCursor, CursorErrorCode> decode(String token) {
        String raw;
        try {
            raw = new String(BASE64_DECODER.decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Result.failure(CursorErrorCode.INVALID_CURSOR);
        }

        int lastColon = raw.lastIndexOf(':');
        if (lastColon < 0) return Result.failure(CursorErrorCode.INVALID_CURSOR);

        String payload = raw.substring(0, lastColon);
        String providedSig = raw.substring(lastColon + 1);

        if (!constantTimeEquals(hmacPrefix(payload), providedSig)) {
            return Result.failure(CursorErrorCode.INVALID_CURSOR);
        }

        int colonIdx = payload.indexOf(':');
        if (colonIdx < 0) return Result.failure(CursorErrorCode.INVALID_CURSOR);

        try {
            long epochMs = Long.parseLong(payload.substring(0, colonIdx));
            long id = Long.parseLong(payload.substring(colonIdx + 1));
            return Result.success(new ParticipationLogCursor(
                    Instant.ofEpochMilli(epochMs), ParticipationLogId.of(id)));
        } catch (NumberFormatException e) {
            return Result.failure(CursorErrorCode.INVALID_CURSOR);
        }
    }

    private String hmacPrefix(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(HMAC_BYTES * 2);
            for (int i = 0; i < HMAC_BYTES; i++) {
                sb.append(String.format("%02x", hmacBytes[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
