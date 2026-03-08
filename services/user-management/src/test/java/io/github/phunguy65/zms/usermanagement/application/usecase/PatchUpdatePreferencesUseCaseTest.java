package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.PatchPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.UserPreferencesResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
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
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PatchUpdatePreferencesUseCaseTest {

    @Mock
    UserRepository userRepository;

    PatchUpdatePreferencesUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new PatchUpdatePreferencesUseCase(
                userRepository, new UserPreferencesParser(new ObjectMapper()), new ObjectMapper());
    }

    private User buildUser(String prefsJson) {
        return User.reconstitute(
                USER_ID,
                Email.of("alice@example.com"),
                null,
                FullName.of("Alice"),
                null,
                null,
                "EMAIL",
                prefsJson,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void execute_mergesNewKeysIntoExisting() throws Exception {
        String stored =
                new ObjectMapper().writeValueAsString(Map.of("theme", "dark", "fontSize", 14));
        var user = buildUser(stored);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new PatchPreferencesRequest(JsonNullable.of(Map.of("lang", "vi")));
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesResponse) ((Result.Success<?, ?>) result).value();
        // existing keys preserved
        assertThat(prefs.settings()).containsEntry("theme", "dark");
        assertThat(prefs.settings()).containsEntry("fontSize", 14);
        // new key added
        assertThat(prefs.settings()).containsEntry("lang", "vi");
    }

    @Test
    void execute_overwritesExistingKey() throws Exception {
        String stored =
                new ObjectMapper().writeValueAsString(Map.of("theme", "dark", "fontSize", 14));
        var user = buildUser(stored);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new PatchPreferencesRequest(JsonNullable.of(Map.of("theme", "light")));
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesResponse) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.settings()).containsEntry("theme", "light");
        assertThat(prefs.settings()).containsEntry("fontSize", 14); // unchanged
    }

    @Test
    void execute_undefinedSettings_returnsCurrentState() {
        var user = buildUser(null);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));

        var dto = new PatchPreferencesRequest(); // undefined
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesResponse) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.settings()).isEmpty();
    }

    @Test
    void execute_nullSettings_clearsAllPreferences() {
        var user = buildUser("{\"theme\":\"dark\"}");
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new PatchPreferencesRequest(JsonNullable.of(null));
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesResponse) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.settings()).isEmpty();
    }

    @Test
    void execute_emptyPatch_keepsExistingKeys() throws Exception {
        String stored = new ObjectMapper().writeValueAsString(Map.of("theme", "dark"));
        var user = buildUser(stored);
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = new PatchPreferencesRequest(JsonNullable.of(Map.of()));
        var result = useCase.execute(USER_ID, dto);

        assertThat(result).isInstanceOf(Result.Success.class);
        var prefs = (UserPreferencesResponse) ((Result.Success<?, ?>) result).value();
        assertThat(prefs.settings()).containsEntry("theme", "dark");
    }

    @Test
    void execute_userNotFound_returnsFailure() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.empty());

        var result = useCase.execute(USER_ID, new PatchPreferencesRequest());

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }
}
