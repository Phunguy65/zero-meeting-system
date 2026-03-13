package io.github.phunguy65.zms.usermanagement.application.command;

import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;

public record PatchPreferencesCommand(JsonNullable<Map<String, Object>> settings) {}
