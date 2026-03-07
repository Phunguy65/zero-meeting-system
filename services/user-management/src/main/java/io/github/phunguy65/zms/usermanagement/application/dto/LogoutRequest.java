package io.github.phunguy65.zms.usermanagement.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {}
