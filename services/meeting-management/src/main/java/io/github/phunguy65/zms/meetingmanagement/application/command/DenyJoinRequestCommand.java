package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;

/**
 * Command to deny a single join request.
 */
public record DenyJoinRequestCommand(UUID meetingId, UUID requestId, UUID deniedBy) {}
