package io.github.phunguy65.zms.usermanagement.application.dto;

import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * PATCH request DTO for merging preference keys. Present keys are merged into the stored
 * preferences object (RFC 7386 JSON Merge Patch semantics); absent fields are left unchanged.
 * A {@code null} value for {@code settings} clears all preferences.
 */
public record PatchPreferencesRequest(JsonNullable<Map<String, Object>> settings) {

    public PatchPreferencesRequest {
        if (settings == null) settings = JsonNullable.undefined();
    }

    public PatchPreferencesRequest() {
        this(JsonNullable.undefined());
    }
}
