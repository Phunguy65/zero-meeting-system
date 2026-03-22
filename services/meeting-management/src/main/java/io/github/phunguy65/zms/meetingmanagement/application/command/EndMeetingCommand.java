package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;

public record EndMeetingCommand(UUID meetingId, UUID requesterId) {}
