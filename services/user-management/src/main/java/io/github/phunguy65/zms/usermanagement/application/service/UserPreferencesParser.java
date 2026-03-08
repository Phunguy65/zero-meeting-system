package io.github.phunguy65.zms.usermanagement.application.service;

import io.github.phunguy65.zms.usermanagement.application.dto.UserPreferencesRequest;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Application service that parses a user's JSON preferences string into a
 * {@link UserPreferencesRequest}, falling back to defaults on any error.
 */
@Service
public class UserPreferencesParser {

    private static final Logger log = LoggerFactory.getLogger(UserPreferencesParser.class);

    private final ObjectMapper objectMapper;

    public UserPreferencesParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UserPreferencesRequest parse(Optional<String> preferencesJson) {
        return preferencesJson
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try {
                        return objectMapper.readValue(s, UserPreferencesRequest.class);
                    } catch (Exception e) {
                        log.warn(
                                "Failed to parse user preferences JSON, returning defaults: {}",
                                e.getMessage());
                        return UserPreferencesRequest.defaults();
                    }
                })
                .orElseGet(UserPreferencesRequest::defaults);
    }
}
