package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;

public record CancelMeetingCommand(UUID meetingId, UUID requesterId) {}
