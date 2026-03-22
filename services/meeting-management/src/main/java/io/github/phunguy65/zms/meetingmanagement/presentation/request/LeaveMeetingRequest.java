package io.github.phunguy65.zms.meetingmanagement.presentation.request;

import io.github.phunguy65.zms.meetingmanagement.application.command.LeaveMeetingCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record LeaveMeetingRequest(@NotBlank String deviceId) {

    public LeaveMeetingCommand toCommand(UUID meetingId) {
        return new LeaveMeetingCommand(meetingId, deviceId);
    }
}
