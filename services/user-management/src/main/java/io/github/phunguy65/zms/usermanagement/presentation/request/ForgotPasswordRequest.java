package io.github.phunguy65.zms.usermanagement.presentation.request;

import io.github.phunguy65.zms.usermanagement.application.command.RequestPasswordResetCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the forgot-password endpoint.
 */
public record ForgotPasswordRequest(@NotBlank @Email String email) {

    public RequestPasswordResetCommand toCommand(String ipAddress) {
        return new RequestPasswordResetCommand(email, ipAddress);
    }
}
