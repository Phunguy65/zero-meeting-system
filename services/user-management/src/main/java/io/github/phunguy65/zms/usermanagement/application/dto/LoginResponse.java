package io.github.phunguy65.zms.usermanagement.application.dto;

public record LoginResponse(String accessToken, String refreshToken, long expiresIn) {}
