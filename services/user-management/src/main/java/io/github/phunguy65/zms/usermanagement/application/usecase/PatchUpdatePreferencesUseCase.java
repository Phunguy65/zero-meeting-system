package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.PatchPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.UserPreferencesResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class PatchUpdatePreferencesUseCase {

    private static final Logger log = LoggerFactory.getLogger(PatchUpdatePreferencesUseCase.class);

    private final UserRepository userRepository;
    private final UserPreferencesParser preferencesParser;
    private final ObjectMapper objectMapper;

    public PatchUpdatePreferencesUseCase(
            UserRepository userRepository,
            UserPreferencesParser preferencesParser,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.preferencesParser = preferencesParser;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Result<UserPreferencesResponse, AuthErrorCode> execute(
            UUID userId, PatchPreferencesRequest dto) {
        var userOpt = userRepository.findActiveById(userId);
        if (userOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.USER_NOT_FOUND);
        }
        var user = userOpt.get();

        if (!dto.settings().isPresent()) {
            return Result.success(preferencesParser.parseAsResponse(user.getPreferences()));
        }

        Map<String, Object> patch = dto.settings().get();

        Map<String, Object> current = new HashMap<>(
                preferencesParser.parseAsResponse(user.getPreferences()).settings());
        if (patch != null) {
            current.putAll(patch);
        } else {
            current.clear();
        }

        try {
            String json = current.isEmpty() ? null : objectMapper.writeValueAsString(current);
            user.updatePreferences(json);
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to serialise preferences for user {}", userId, e);
            return Result.failure(AuthErrorCode.USER_NOT_FOUND);
        }

        return Result.success(new UserPreferencesResponse(current));
    }
}
