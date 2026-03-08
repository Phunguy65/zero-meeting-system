package io.github.phunguy65.zms.usermanagement.application.dto;

import java.util.Collections;
import java.util.Map;

/**
 * Free-form preferences submitted by the client. The server imposes no schema constraints;
 * any valid JSON object is accepted and stored as-is.
 */
public record UserPreferencesRequest(Map<String, Object> settings) {

    public UserPreferencesRequest {
        settings =
                settings != null ? Collections.unmodifiableMap(settings) : Collections.emptyMap();
    }

    /** Returns an empty preferences request (no settings). */
    public static UserPreferencesRequest empty() {
        return new UserPreferencesRequest(Collections.emptyMap());
    }
}
