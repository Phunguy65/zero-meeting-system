package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.GoogleLoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginResponse;
import io.github.phunguy65.zms.usermanagement.application.service.RefreshTokenIssuer;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.GoogleAuthClaims;
import io.github.phunguy65.zms.usermanagement.domain.model.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.GoogleAuthVerifier;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.TokenProvider;
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
class LoginWithGoogleUseCaseTest {

    @Mock
    GoogleAuthVerifier googleAuthVerifier;

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    TokenProvider tokenProvider;

    LoginWithGoogleUseCase useCase;

    private static final String GOOGLE_UID = "google-uid-123";
    private static final String EMAIL = "alice@example.com";
    private static final String ID_TOKEN = "firebase.id.token";
    private static final GoogleAuthClaims CLAIMS =
            new GoogleAuthClaims(GOOGLE_UID, EMAIL, "Alice", "https://photo.url");

    @BeforeEach
    void setUp() {
        var refreshTokenIssuer = new RefreshTokenIssuer(refreshTokenRepository);
        var preferencesParser = new UserPreferencesParser(new ObjectMapper());
        useCase = new LoginWithGoogleUseCase(
                googleAuthVerifier,
                userRepository,
                tokenProvider,
                refreshTokenIssuer,
                preferencesParser,
                2592000L);
    }

    @Test
    void invalidTokenReturnsFailure() {
        when(googleAuthVerifier.verify(ID_TOKEN))
                .thenReturn(Result.failure(AuthErrorCode.INVALID_FIREBASE_TOKEN));

        var result = useCase.execute(new GoogleLoginRequest(ID_TOKEN));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.INVALID_FIREBASE_TOKEN);
        verifyNoInteractions(userRepository);
    }

    @Test
    void firstTimeLoginCreatesNewUser() {
        when(googleAuthVerifier.verify(ID_TOKEN)).thenReturn(Result.success(CLAIMS));
        when(userRepository.findActiveByGoogleUid(GOOGLE_UID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(Email.of(EMAIL))).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any(), any())).thenReturn("access.token");
        when(tokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);

        var result = useCase.execute(new GoogleLoginRequest(ID_TOKEN));

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = (LoginResponse) ((Result.Success<?, ?>) result).value();
        assertThat(response.accessToken()).isEqualTo("access.token");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void returningGoogleUserSkipsCreation() {
        var existingUser = User.reconstitute(
                UUID.randomUUID(),
                Email.of(EMAIL),
                null,
                FullName.of("Alice"),
                null,
                GOOGLE_UID,
                "GOOGLE",
                null,
                Instant.now(),
                Instant.now(),
                null);

        when(googleAuthVerifier.verify(ID_TOKEN)).thenReturn(Result.success(CLAIMS));
        when(userRepository.findActiveByGoogleUid(GOOGLE_UID))
                .thenReturn(Optional.of(existingUser));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any(), any())).thenReturn("access.token");
        when(tokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);

        var result = useCase.execute(new GoogleLoginRequest(ID_TOKEN));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void accountLinkingForExistingEmailUser() {
        var emailUser = User.reconstitute(
                UUID.randomUUID(),
                Email.of(EMAIL),
                HashedPassword.of("$argon2id$hash"),
                FullName.of("Alice"),
                null,
                null,
                "EMAIL",
                null,
                Instant.now(),
                Instant.now(),
                null);

        when(googleAuthVerifier.verify(ID_TOKEN)).thenReturn(Result.success(CLAIMS));
        when(userRepository.findActiveByGoogleUid(GOOGLE_UID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(Email.of(EMAIL))).thenReturn(Optional.of(emailUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateAccessToken(any(), any())).thenReturn("access.token");
        when(tokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);

        var result = useCase.execute(new GoogleLoginRequest(ID_TOKEN));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(userRepository).save(argThat(u -> "BOTH".equals(u.getAuthProvider())));
    }

    @Test
    void deletedUserReturnsFailure() {
        var deletedUser = User.reconstitute(
                UUID.randomUUID(),
                Email.of(EMAIL),
                null,
                FullName.of("Alice"),
                null,
                null,
                "EMAIL",
                null,
                Instant.now(),
                Instant.now(),
                Instant.now()); // deleted

        when(googleAuthVerifier.verify(ID_TOKEN)).thenReturn(Result.success(CLAIMS));
        when(userRepository.findActiveByGoogleUid(GOOGLE_UID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(Email.of(EMAIL))).thenReturn(Optional.of(deletedUser));

        var result = useCase.execute(new GoogleLoginRequest(ID_TOKEN));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.USER_DELETED);
    }
}
