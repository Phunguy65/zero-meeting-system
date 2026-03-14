package io.github.phunguy65.zms.usermanagement.presentation.request;

import io.github.phunguy65.zms.usermanagement.application.command.GoogleLoginCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/google-login}.
 */
public record GoogleLoginRequest(@NotBlank String idToken) {

    public GoogleLoginCommand toCommand() {
        return new GoogleLoginCommand(idToken);
    }
}
