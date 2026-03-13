package io.github.phunguy65.zms.usermanagement.application.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserPreferencesResponse preferences) {}
