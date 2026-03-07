package io.github.phunguy65.zms.usermanagement.application.dto;

import java.time.Instant;
import java.util.UUID;

public record DeleteAccountResponse(
        UUID userId, String email, String fullName, Instant deletedAt) {}
