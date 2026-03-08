package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.UserPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class UpdateUserPreferencesUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserPreferencesUseCase.class);

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public UpdateUserPreferencesUseCase(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Result<UserPreferencesRequest, AuthErrorCode> execute(
            UUID userId, UserPreferencesRequest dto) {
        var userOpt = userRepository.findActiveById(userId);
        if (userOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.USER_NOT_FOUND);
        }
        var user = userOpt.get();

        try {
            String json = objectMapper.writeValueAsString(dto);
            user.updatePreferences(json);
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to serialise preferences for user {}", userId, e);
            return Result.failure(AuthErrorCode.USER_NOT_FOUND);
        }

        return Result.success(dto);
    }
}
