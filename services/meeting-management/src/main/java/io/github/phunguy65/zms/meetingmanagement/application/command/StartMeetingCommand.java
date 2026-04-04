package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;

public record StartMeetingCommand(UUID meetingId, UUID requesterId) {}
