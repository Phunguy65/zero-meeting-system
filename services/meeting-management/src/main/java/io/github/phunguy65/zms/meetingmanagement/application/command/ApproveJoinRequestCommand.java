package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;

/**
 * Command to approve a single join request.
 */
public record ApproveJoinRequestCommand(UUID meetingId, UUID requestId, UUID approvedBy) {}
