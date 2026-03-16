package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.command.PatchUserCommand;
import io.github.phunguy65.zms.usermanagement.application.helper.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.application.response.UserResponse;
import io.github.phunguy65.zms.usermanagement.domain.AuthError;
import io.github.phunguy65.zms.usermanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.Username;
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
    public Result<UserResponse, AuthError> execute(UUID userId, PatchUserCommand command) {
        var userOpt = userRepository.findActiveById(userId);
        if (userOpt.isEmpty()) {
            return Result.failure(new AuthError.UserNotFound());
        }
        var user = userOpt.get();

        boolean anyChange = command.fullName().isPresent()
                || command.avatarUrl().isPresent()
                || command.username().isPresent()
                || command.preferences().isPresent();

        if (!anyChange) {
            return Result.success(toResponse(user));
        }

        Username newUsername = null;
        if (command.username().isPresent()) {
            String rawUsername = command.username().get();
            if (rawUsername != null) {
                final Username candidateUsername = Username.of(rawUsername);
                boolean isSameAsCurrent = user.getUsername()
                        .map(u -> u.value().equals(candidateUsername.value()))
                        .orElse(false);
                if (!isSameAsCurrent && userRepository.existsActiveByUsername(candidateUsername)) {
                    return Result.failure(new AuthError.UsernameAlreadyExists());
                }
                newUsername = candidateUsername;
            }
        }

        if (command.fullName().isPresent()
                || command.avatarUrl().isPresent()
                || newUsername != null) {
            FullName newFullName = command.fullName().isPresent()
                    ? FullName.of(command.fullName().get())
                    : null;
            boolean applyAvatar = command.avatarUrl().isPresent();
            String newAvatarUrl = applyAvatar ? command.avatarUrl().get() : null;
            user.updateProfile(newFullName, newAvatarUrl, applyAvatar, newUsername);
        }

        if (command.preferences().isPresent()) {
            try {
                String json = command.preferences().get() != null
                        ? objectMapper.writeValueAsString(command.preferences().get())
                        : null;
                user.updatePreferences(json);
            } catch (Exception e) {
                log.error("Failed to serialise preferences for user {}", userId, e);
                return Result.failure(new AuthError.PreferencesSerializationError());
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
                user.getUsername().map(Username::value).orElse(null),
                user.getAvatarUrl().orElse(null),
                user.getAuthProvider(),
                preferencesParser.parseAsResponse(user.getPreferences()),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
