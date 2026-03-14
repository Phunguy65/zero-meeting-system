package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.helper.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.application.response.UserPreferencesResponse;
import io.github.phunguy65.zms.usermanagement.domain.AuthError;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserPreferencesUseCase {

    private final UserRepository userRepository;
    private final UserPreferencesParser preferencesParser;

    public GetUserPreferencesUseCase(
            UserRepository userRepository, UserPreferencesParser preferencesParser) {
        this.userRepository = userRepository;
        this.preferencesParser = preferencesParser;
    }

    @Transactional(readOnly = true)
    public Result<UserPreferencesResponse, AuthError> execute(UUID userId) {
        var userOpt = userRepository.findActiveById(userId);
        if (userOpt.isEmpty()) {
            return Result.failure(new AuthError.UserNotFound());
        }
        return Result.success(preferencesParser.parseAsResponse(userOpt.get().getPreferences()));
    }
}
