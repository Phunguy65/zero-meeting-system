package io.github.phunguy65.zms.meetingmanagement.application.response;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.ParticipantSummary;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ParticipantListItemResponse(
        Long id,
        UUID meetingId,
        @Nullable UUID userId,
        String displayName,
        ParticipantRole role,
        Instant joinedAt,
        @Nullable Instant leftAt) {

    public static ParticipantListItemResponse fromProjection(ParticipantSummary projection) {
        return new ParticipantListItemResponse(
                projection.id(),
                projection.meetingId(),
                projection.userId(),
                projection.displayName(),
                ParticipantRole.valueOf(projection.role()),
                projection.joinedAt(),
                projection.leftAt());
    }
}
