package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;

/** Participant information shown on the meeting detail screen. */
public record MeetingParticipant(
        String userId,
        String displayName,
        String role,
        OffsetDateTime joinedAt,
        OffsetDateTime leftAt) {}
