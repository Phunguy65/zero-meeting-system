package io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject;

import java.time.Instant;

/**
 * Decoded keyset cursor for participation log pagination.
 *
 * <p>Uses {@code joinedAt} + {@link ParticipationLogId} (bigserial) for stable ordering.
 */
public record ParticipationLogCursor(Instant joinedAt, ParticipationLogId id) {}
