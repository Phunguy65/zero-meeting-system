package io.github.phunguy65.zms.usermanagement.presentation.request;

import io.github.phunguy65.zms.usermanagement.application.command.LoginCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email String email, @NotBlank String password) {

    public LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }
}
