package io.github.phunguy65.zms.usermanagement.presentation.request;

import io.github.phunguy65.zms.usermanagement.application.command.ResetPasswordCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the reset-password endpoint.
 */
public record ResetPasswordRequest(
        @NotBlank @Email String email,

        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits") String otp,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String newPassword) {

    public ResetPasswordCommand toCommand() {
        return new ResetPasswordCommand(email, otp, newPassword);
    }
}
