package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;

public record StopRecordingCommand(UUID meetingId, UUID requesterId) {}
