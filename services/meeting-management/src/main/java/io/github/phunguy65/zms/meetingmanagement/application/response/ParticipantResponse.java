package io.github.phunguy65.zms.meetingmanagement.application.response;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogId;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ParticipantResponse(
        ParticipationLogId id,
        UUID meetingId,
        @Nullable UUID userId,
        String displayName,
        ParticipantRole role,
        Instant joinedAt,
        @Nullable Instant leftAt) {}
