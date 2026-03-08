package io.github.phunguy65.zms.usermanagement.infrastructure.security;

import org.jspecify.annotations.Nullable;

/**
 * Verified claims extracted from a Firebase ID token.
 */
public record FirebaseTokenClaims(
        String uid,
        String email,
        @Nullable String displayName,
        @Nullable String photoUrl) {}
