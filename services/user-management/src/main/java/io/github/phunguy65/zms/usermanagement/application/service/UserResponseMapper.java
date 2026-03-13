package io.github.phunguy65.zms.usermanagement.application.service;

import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.model.Username;
import org.springframework.stereotype.Service;

/**
 * Maps {@link User} domain objects to {@link UserResponse} DTOs.
 *
 * <p>Centralises the mapping logic that was previously duplicated across
 * {@code GetUserUseCase}, {@code PatchUpdateUserUseCase}, and {@code SearchUsersUseCase}.
 */
@Service
public class UserResponseMapper {

    private final UserPreferencesParser preferencesParser;

    public UserResponseMapper(UserPreferencesParser preferencesParser) {
        this.preferencesParser = preferencesParser;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail().value(),
                user.getFullName().value(),
                user.getUsername().map(Username::value).orElse(null),
                user.getAvatarUrl().orElse(null),
                user.getAuthProvider(),
                preferencesParser.parseAsResponse(user.getPreferences()),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
