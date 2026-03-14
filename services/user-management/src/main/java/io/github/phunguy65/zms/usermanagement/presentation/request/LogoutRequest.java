package io.github.phunguy65.zms.usermanagement.presentation.request;

import io.github.phunguy65.zms.usermanagement.application.command.LogoutCommand;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {

    public LogoutCommand toCommand() {
        return new LogoutCommand(refreshToken);
    }
}
