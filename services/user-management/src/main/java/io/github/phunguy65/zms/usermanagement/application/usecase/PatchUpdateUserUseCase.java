package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.PatchUserRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class PatchUpdateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(PatchUpdateUserUseCase.class);

    private final UserRepository userRepository;
    private final UserPreferencesParser preferencesParser;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public PatchUpdateUserUseCase(
            UserRepository userRepository,
            UserPreferencesParser preferencesParser,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.preferencesParser = preferencesParser;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Result<UserResponse, AuthErrorCode> execute(UUID userId, PatchUserRequest dto) {
        var userOpt = userRepository.findActiveById(userId);
        if (userOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.USER_NOT_FOUND);
        }
        var user = userOpt.get();

        boolean anyChange = dto.fullName().isPresent()
                || dto.avatarUrl().isPresent()
                || dto.preferences().isPresent();

        if (!anyChange) {
            return Result.success(toResponse(user));
        }

        if (dto.fullName().isPresent() || dto.avatarUrl().isPresent()) {
            FullName newFullName =
                    dto.fullName().isPresent() ? FullName.of(dto.fullName().get()) : null;
            boolean applyAvatar = dto.avatarUrl().isPresent();
            String newAvatarUrl = applyAvatar ? dto.avatarUrl().get() : null;
            user.updateProfile(newFullName, newAvatarUrl, applyAvatar);
        }

        if (dto.preferences().isPresent()) {
            try {
                String json = dto.preferences().get() != null
                        ? objectMapper.writeValueAsString(dto.preferences().get())
                        : null;
                user.updatePreferences(json);
            } catch (Exception e) {
                log.error("Failed to serialise preferences for user {}", userId, e);
                return Result.failure(AuthErrorCode.USER_NOT_FOUND);
            }
        }

        var saved = userRepository.save(user);

        saved.getDomainEvents().stream()
                .filter(e -> e instanceof PublishableEvent)
                .map(e -> (PublishableEvent) e)
                .forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();

        return Result.success(toResponse(saved));
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
