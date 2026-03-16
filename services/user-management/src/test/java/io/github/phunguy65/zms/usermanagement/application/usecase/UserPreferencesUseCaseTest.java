package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.helper.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.application.response.UserPreferencesResponse;
import io.github.phunguy65.zms.usermanagement.domain.AuthError;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.FullName;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.time.Instant;
import java.util.Map;
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

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        var preferencesParser = new UserPreferencesParser(new ObjectMapper());
        getUseCase = new GetUserPreferencesUseCase(userRepository, preferencesParser);
    }

    private User userWithPrefs(String prefsJson) {
        return User.reconstitute(
                USER_ID,
                Email.of("alice@example.com"),
                null,
                FullName.of("Alice"),
                null,
                null,
                "google-uid",
                "GOOGLE",
                prefsJson,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void getPreferences_nullPrefs_returnsEmpty() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(userWithPrefs(null)));

        var result = getUseCase.execute(USER_ID);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesResponse) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.settings()).isEmpty();
    }

    @Test
    void getPreferences_storedPrefs_returnsStoredAsIs() throws Exception {
        String json = new ObjectMapper()
                .writeValueAsString(Map.of("theme", "dark", "fontSize", 14, "lang", "vi"));
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(userWithPrefs(json)));

        var result = getUseCase.execute(USER_ID);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesResponse) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.settings()).containsEntry("theme", "dark");
        assertThat(prefs.settings()).containsEntry("fontSize", 14);
        assertThat(prefs.settings()).containsEntry("lang", "vi");
    }

    @Test
    void getPreferences_arbitraryKeys_accepted() throws Exception {
        String json = new ObjectMapper()
                .writeValueAsString(Map.of("customKey", "customValue", "nested", Map.of("a", 1)));
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(userWithPrefs(json)));

        var result = getUseCase.execute(USER_ID);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesResponse) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.settings()).containsKey("customKey");
        assertThat(prefs.settings()).containsKey("nested");
    }

    @Test
    void getPreferences_userNotFound_returnsFailure() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.empty());

        var result = getUseCase.execute(USER_ID);

        assertThat(((Result.Failure<?, AuthError>) result).error())
                .isInstanceOf(AuthError.UserNotFound.class);
    }
}
