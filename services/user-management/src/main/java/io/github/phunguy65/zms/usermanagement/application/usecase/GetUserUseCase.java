package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserUseCase {

    private final UserRepository userRepository;
    private final UserPreferencesParser preferencesParser;

    public GetUserUseCase(UserRepository userRepository, UserPreferencesParser preferencesParser) {
        this.userRepository = userRepository;
        this.preferencesParser = preferencesParser;
    }

    @Transactional(readOnly = true)
    public Result<UserResponse, AuthErrorCode> execute(UUID userId) {
        return userRepository
                .findActiveById(userId)
                .map(user -> Result.<UserResponse, AuthErrorCode>success(toResponse(user)))
                .orElseGet(() -> Result.failure(AuthErrorCode.USER_NOT_FOUND));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail().value(),
                user.getFullName().value(),
                user.getAvatarUrl().orElse(null),
                user.getAuthProvider(),
                preferencesParser.parseAsResponse(user.getPreferences()),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
