package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.Email;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import io.github.phunguy65.zms.usermanagement.application.command.RequestPasswordResetCommand;
import io.github.phunguy65.zms.usermanagement.domain.AuthError;
import io.github.phunguy65.zms.usermanagement.domain.event.PasswordResetRequestedEvent;
import io.github.phunguy65.zms.usermanagement.domain.model.PasswordResetToken;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.port.OtpGenerator;
import io.github.phunguy65.zms.usermanagement.domain.port.OtpHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordResetRateLimiter;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordResetTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetUseCaseTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordResetTokenRepository tokenRepository;

    @Mock
    PasswordResetRateLimiter rateLimiter;

    @Mock
    OtpGenerator otpGenerator;

    @Mock
    OtpHasher otpHasher;

    @Mock
    ApplicationEventPublisher eventPublisher;

    RequestPasswordResetUseCase useCase;

    private User testUser;
    private User googleOnlyUser;

    @BeforeEach
    void setUp() {
        useCase = new RequestPasswordResetUseCase(
                userRepository,
                tokenRepository,
                rateLimiter,
                otpGenerator,
                otpHasher,
                eventPublisher);

        testUser = User.reconstitute(
                UserId.of(UuidCreator.getTimeOrderedEpoch()),
                Email.of("alice@example.com"),
                HashedPassword.of("$argon2id$hash"),
                FullName.of("Alice Smith"),
                null,
                null,
                null,
                "EMAIL",
                null,
                Instant.now(),
                Instant.now(),
                null);

        googleOnlyUser = User.reconstitute(
                UserId.of(UuidCreator.getTimeOrderedEpoch()),
                Email.of("google@example.com"),
                null, // no password
                FullName.of("Google User"),
                null,
                null,
                "google-uid-abc",
                "GOOGLE",
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Nested
    class WhenValidEmailWithPasswordAccount {

        @BeforeEach
        void setUp() {
            when(rateLimiter.isAllowed(any(), any())).thenReturn(true);
            when(userRepository.findActiveByEmail(Email.of("alice@example.com")))
                    .thenReturn(Optional.of(testUser));
            when(otpGenerator.generate()).thenReturn("123456");
            when(otpHasher.hash("123456")).thenReturn("sha256hash");
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        void returnsSuccess() {
            var result = useCase.execute(
                    new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            assertThat(result).isInstanceOf(Result.Success.class);
        }

        @Test
        void generates6DigitOtp() {
            useCase.execute(new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            verify(otpGenerator).generate();
        }

        @Test
        void hashesOtpWithSha256() {
            useCase.execute(new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            verify(otpHasher).hash("123456");
        }

        @Test
        void savesPasswordResetTokenWith15MinuteExpiry() {
            useCase.execute(new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            ArgumentCaptor<PasswordResetToken> captor =
                    ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());

            PasswordResetToken savedToken = captor.getValue();
            assertThat(savedToken.getUserId()).isEqualTo(testUser.getId());
            assertThat(savedToken.getOtpHash()).isEqualTo("sha256hash");
            assertThat(savedToken.getAttempts()).isZero();
            // Expiry should be ~15 minutes from now
            assertThat(savedToken.getExpiresAt())
                    .isBetween(
                            Instant.now().plusSeconds(14 * 60), Instant.now().plusSeconds(16 * 60));
        }

        @Test
        void publishesPasswordResetRequestedEvent() {
            useCase.execute(new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            ArgumentCaptor<PasswordResetRequestedEvent> captor =
                    ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            PasswordResetRequestedEvent event = captor.getValue();
            assertThat(event.userId()).isEqualTo(testUser.getId().value());
            assertThat(event.email()).isEqualTo("alice@example.com");
            assertThat(event.fullName()).isEqualTo("Alice Smith");
            assertThat(event.otp()).isEqualTo("123456"); // Plaintext OTP for email
        }

        @Test
        void invalidatesExistingTokensBeforeCreatingNew() {
            useCase.execute(new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            verify(tokenRepository).invalidateAllByUserId(testUser.getId());
            verify(tokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        void recordsAttemptForRateLimiting() {
            useCase.execute(new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            verify(rateLimiter).recordAttempt("alice@example.com", "192.168.1.1");
        }

        @Test
        void normalizesEmailToLowercase() {
            when(userRepository.findActiveByEmail(Email.of("alice@example.com")))
                    .thenReturn(Optional.of(testUser));

            useCase.execute(new RequestPasswordResetCommand("ALICE@EXAMPLE.COM", "192.168.1.1"));

            verify(userRepository).findActiveByEmail(Email.of("alice@example.com"));
        }
    }

    @Nested
    class WhenGoogleOnlyAccount {

        @BeforeEach
        void setUp() {
            when(rateLimiter.isAllowed(any(), any())).thenReturn(true);
            when(userRepository.findActiveByEmail(Email.of("google@example.com")))
                    .thenReturn(Optional.of(googleOnlyUser));
        }

        @Test
        void returnsSuccessWithoutSendingOtp() {
            var result = useCase.execute(
                    new RequestPasswordResetCommand("google@example.com", "192.168.1.1"));

            assertThat(result).isInstanceOf(Result.Success.class);
            verify(eventPublisher, never()).publishEvent(any());
            verify(tokenRepository, never()).save(any());
        }

        @Test
        void stillRecordsAttemptForRateLimiting() {
            useCase.execute(new RequestPasswordResetCommand("google@example.com", "192.168.1.1"));

            verify(rateLimiter).recordAttempt("google@example.com", "192.168.1.1");
        }
    }

    @Nested
    class WhenEmailNotFound {

        @BeforeEach
        void setUp() {
            when(rateLimiter.isAllowed(any(), any())).thenReturn(true);
            when(userRepository.findActiveByEmail(any())).thenReturn(Optional.empty());
        }

        @Test
        void returnsSuccessToPreventEnumeration() {
            var result = useCase.execute(
                    new RequestPasswordResetCommand("nobody@example.com", "192.168.1.1"));

            assertThat(result).isInstanceOf(Result.Success.class);
        }

        @Test
        void doesNotSendEmail() {
            useCase.execute(new RequestPasswordResetCommand("nobody@example.com", "192.168.1.1"));

            verify(eventPublisher, never()).publishEvent(any());
            verify(tokenRepository, never()).save(any());
        }

        @Test
        void stillRecordsAttemptForRateLimiting() {
            useCase.execute(new RequestPasswordResetCommand("nobody@example.com", "192.168.1.1"));

            verify(rateLimiter).recordAttempt("nobody@example.com", "192.168.1.1");
        }
    }

    @Nested
    class WhenRateLimitExceeded {

        @Test
        void returnsRateLimitExceededError() {
            when(rateLimiter.isAllowed("alice@example.com", "192.168.1.1")).thenReturn(false);

            var result = useCase.execute(
                    new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            assertThat(result).isInstanceOf(Result.Failure.class);
            assertThat(((Result.Failure<?, AuthError>) result).error())
                    .isInstanceOf(AuthError.RateLimitExceeded.class);
        }

        @Test
        void checksRateLimitBeforeAnyDatabaseOperations() {
            when(rateLimiter.isAllowed(any(), any())).thenReturn(false);

            useCase.execute(new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            verify(userRepository, never()).findActiveByEmail(any());
            verify(tokenRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void doesNotRecordAttemptWhenRateLimited() {
            when(rateLimiter.isAllowed(any(), any())).thenReturn(false);

            useCase.execute(new RequestPasswordResetCommand("alice@example.com", "192.168.1.1"));

            verify(rateLimiter, never()).recordAttempt(any(), any());
        }
    }

    @Nested
    class RateLimitChecks {

        @Test
        void checksRateLimitWithNormalizedEmail() {
            when(rateLimiter.isAllowed("alice@example.com", "192.168.1.1")).thenReturn(true);
            when(userRepository.findActiveByEmail(any())).thenReturn(Optional.empty());

            useCase.execute(new RequestPasswordResetCommand("ALICE@EXAMPLE.COM  ", "192.168.1.1"));

            verify(rateLimiter).isAllowed("alice@example.com", "192.168.1.1");
        }
    }
}
