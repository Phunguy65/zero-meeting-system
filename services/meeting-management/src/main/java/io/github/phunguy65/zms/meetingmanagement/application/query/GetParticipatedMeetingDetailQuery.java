package io.github.phunguy65.zms.meetingmanagement.application.query;

import java.util.UUID;

public record GetParticipatedMeetingDetailQuery(UUID userId, UUID meetingId, UUID requesterId) {}
