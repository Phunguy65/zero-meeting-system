package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;

/**
 * Command to approve all pending join requests for a meeting.
 */
public record ApproveAllJoinRequestsCommand(UUID meetingId, UUID approvedBy) {}
