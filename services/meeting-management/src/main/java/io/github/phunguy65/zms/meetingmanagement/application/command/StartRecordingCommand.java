package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;

public record StartRecordingCommand(UUID meetingId, UUID requesterId) {}
