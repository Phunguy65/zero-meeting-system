package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.UserPreferencesResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
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
    public Result<UserPreferencesResponse, AuthErrorCode> execute(UUID userId) {
        var userOpt = userRepository.findActiveById(userId);
        if (userOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.USER_NOT_FOUND);
        }
        return Result.success(preferencesParser.parseAsResponse(userOpt.get().getPreferences()));
    }
}
