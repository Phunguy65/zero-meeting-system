package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.PatchUserRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.event.UserUpdatedEvent;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatchUpdateUserUseCaseTest {

    @Mock
    UserRepository userRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    PatchUpdateUserUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new PatchUpdateUserUseCase(
                userRepository,
                new UserPreferencesParser(new ObjectMapper()),
                eventPublisher,
                new ObjectMapper());
    }

    private User buildUser(String avatarUrl) {
        return User.reconstitute(
                USER_ID,
                Email.of("alice@example.com"),
                null,
                FullName.of("Alice"),
                avatarUrl,
                null,
                "EMAIL",
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void execute_emptyBody_isNoOpAndNoEvent() {
        var user = buildUser(null);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));

        var dto = new PatchUserRequest();
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void execute_fullNamePresent_updatesFullName() {
        var user = buildUser(null);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new PatchUserRequest(
                JsonNullable.of("New Name"), JsonNullable.undefined(), JsonNullable.undefined());
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = (UserResponse) ((Result.Success<?, ?>) result).value();
        assertThat(response.fullName()).isEqualTo("New Name");
        verify(eventPublisher).publishEvent(any(UserUpdatedEvent.class));
    }

    @Test
    void execute_avatarUrlNull_clearsAvatarUrl() {
        var user = buildUser("https://example.com/old.png");
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new PatchUserRequest(
                JsonNullable.undefined(), JsonNullable.of(null), JsonNullable.undefined());
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = (UserResponse) ((Result.Success<?, ?>) result).value();
        assertThat(response.avatarUrl()).isNull();
    }

    @Test
    void execute_preferencesPresent_replacesPreferences() {
        var user = buildUser(null);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var prefs = java.util.Map.<String, Object>of(
                "theme", "dark", "defaultMic", false, "defaultCamera", true);
        var dto = new PatchUserRequest(
                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.of(prefs));
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void execute_preferencesAbsent_doesNotModifyPreferences() {
        var user = buildUser(null);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new PatchUserRequest(
                JsonNullable.of("Alice Updated"),
                JsonNullable.undefined(),
                JsonNullable.undefined());
        useCase.execute(USER_ID, dto);

        // Verify save was called but preferences were not changed (still null)
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPreferences()).isEmpty();
    }

    @Test
    void execute_userNotFound_returnsFailure() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.empty());

        var result = useCase.execute(USER_ID, new PatchUserRequest());

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }

    @Test
    void execute_updatedEvent_publishedOnChange() {
        var user = buildUser(null);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new PatchUserRequest(
                JsonNullable.of("Updated Name"),
                JsonNullable.undefined(),
                JsonNullable.undefined());
        useCase.execute(USER_ID, dto);

        var captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(UserUpdatedEvent.class);
        var event = (UserUpdatedEvent) captor.getValue();
        assertThat(event.fullName()).isEqualTo("Updated Name");
        assertThat(event.email()).isEqualTo("alice@example.com");
    }
}
