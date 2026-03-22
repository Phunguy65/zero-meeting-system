package io.github.phunguy65.zms.meetingmanagement.application.command;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Command to request joining a meeting (with manual approval if required).
 */
public record RequestJoinCommand(
        UUID meetingId,
        @Nullable UUID userId,
        String displayName,
        String deviceId,
        @Nullable String password) {}
