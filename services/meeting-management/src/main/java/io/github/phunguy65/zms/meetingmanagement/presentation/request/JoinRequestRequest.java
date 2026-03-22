package io.github.phunguy65.zms.meetingmanagement.presentation.request;

import io.github.phunguy65.zms.meetingmanagement.application.command.RequestJoinCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record JoinRequestRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank String deviceId,
        @Nullable @Size(max = 128) String password) {

    public RequestJoinCommand toCommand(UUID meetingId, @Nullable UUID userId) {
        return new RequestJoinCommand(meetingId, userId, displayName, deviceId, password);
    }
}
