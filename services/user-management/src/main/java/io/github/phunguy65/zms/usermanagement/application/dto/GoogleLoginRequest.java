package io.github.phunguy65.zms.usermanagement.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/google-login}.
 */
public record GoogleLoginRequest(@NotBlank String idToken) {}
