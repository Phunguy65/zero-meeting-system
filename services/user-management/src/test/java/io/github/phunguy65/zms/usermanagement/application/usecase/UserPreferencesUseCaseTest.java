package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.UserPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class UserPreferencesUseCaseTest {

    @Mock
    UserRepository userRepository;

    GetUserPreferencesUseCase getUseCase;
    UpdateUserPreferencesUseCase updateUseCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var preferencesParser = new UserPreferencesParser(new ObjectMapper());
        getUseCase = new GetUserPreferencesUseCase(userRepository, preferencesParser);
        updateUseCase = new UpdateUserPreferencesUseCase(userRepository, new ObjectMapper());
    }

    private User userWithPrefs(String prefsJson) {
        return User.reconstitute(
                USER_ID,
                Email.of("alice@example.com"),
                null,
                FullName.of("Alice"),
                null,
                "google-uid",
                "GOOGLE",
                prefsJson,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void getPreferences_nullPrefs_returnsDefaults() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(userWithPrefs(null)));

        var result = getUseCase.execute(USER_ID);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesRequest) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.theme()).isEqualTo("system");
        assertThat(prefs.defaultMic()).isTrue();
        assertThat(prefs.defaultCamera()).isTrue();
    }

    @Test
    void getPreferences_storedPrefs_returnsStored() throws Exception {
        String json = new ObjectMapper()
                .writeValueAsString(new UserPreferencesRequest("dark", false, true));
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(userWithPrefs(json)));

        var result = getUseCase.execute(USER_ID);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesRequest) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.theme()).isEqualTo("dark");
        assertThat(prefs.defaultMic()).isFalse();
    }

    @Test
    void getPreferences_userNotFound_returnsFailure() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.empty());

        var result = getUseCase.execute(USER_ID);

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updatePreferences_happyPath_savesAndReturns() {
        var user = userWithPrefs(null);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new UserPreferencesRequest("light", true, false);
        var result = updateUseCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        var saved = (UserPreferencesRequest) ((Result.Success<?, ?>) result).value();
        assertThat(saved.theme()).isEqualTo("light");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updatePreferences_userNotFound_returnsFailure() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.empty());

        var result = updateUseCase.execute(USER_ID, new UserPreferencesRequest("dark", true, true));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }
}
