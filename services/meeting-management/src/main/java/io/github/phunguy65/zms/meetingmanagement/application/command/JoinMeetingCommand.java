package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record JoinMeetingCommand(
        UUID meetingId,
        @Nullable UUID userId,
        String displayName,
        String deviceId,
        @Nullable String password) {}
