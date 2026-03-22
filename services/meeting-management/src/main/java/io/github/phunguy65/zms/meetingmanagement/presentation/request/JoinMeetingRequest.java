package io.github.phunguy65.zms.meetingmanagement.presentation.request;

import io.github.phunguy65.zms.meetingmanagement.application.command.JoinMeetingCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record JoinMeetingRequest(
        @NotBlank @Size(max = 100) String displayName,
        @Nullable String deviceId,
        @Nullable @Size(max = 128) String password) {

    public JoinMeetingCommand toCommand(UUID meetingId, @Nullable UUID userId) {
        return new JoinMeetingCommand(meetingId, userId, displayName, deviceId, password);
    }
}
