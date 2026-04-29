package io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject;

import java.time.Instant;
import java.util.UUID;

public record ParticipatedMeetingCursor(Instant lastJoinedAt, UUID meetingId) {}
