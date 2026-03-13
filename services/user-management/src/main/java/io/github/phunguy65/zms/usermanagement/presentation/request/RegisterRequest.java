package io.github.phunguy65.zms.usermanagement.presentation.request;

import io.github.phunguy65.zms.usermanagement.application.command.RegisterCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,

        @NotBlank @Size(max = 255) String fullName,

        @NotBlank @Size(min = 3, max = 30) @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "Username must contain only letters, digits, _ or -")
        String username) {

    public RegisterCommand toCommand() {
        return new RegisterCommand(email, password, fullName, username);
    }
}
