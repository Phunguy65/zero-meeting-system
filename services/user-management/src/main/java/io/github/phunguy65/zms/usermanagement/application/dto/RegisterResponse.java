package io.github.phunguy65.zms.usermanagement.application.dto;

import java.util.UUID;

public record RegisterResponse(UUID userId, String email, String fullName) {}
