package io.github.phunguy65.zms.domain.usecase.auth;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.repository.AuthRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link RefreshTokenUseCase}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RefreshTokenUseCaseTest {

    @Mock
    private AuthRepository authRepository;

    private RefreshTokenUseCase useCase;

    @Before
    public void setup() {
        useCase = new RefreshTokenUseCase(authRepository);
    }

    @Test
    public void execute_callsRepository() {
        String refreshToken = "refresh_token_123";
        LoginResult result = new LoginResult("new_access", "new_refresh", 3600);
        when(authRepository.refreshToken(refreshToken))
                .thenReturn(CompletableFuture.completedFuture(result));

        useCase.execute(refreshToken);

        verify(authRepository).refreshToken(refreshToken);
    }

    @Test
    public void execute_success_returnsLoginResult() throws Exception {
        String refreshToken = "refresh_token_123";
        LoginResult expectedResult = new LoginResult("new_access", "new_refresh", 3600);
        when(authRepository.refreshToken(refreshToken))
                .thenReturn(CompletableFuture.completedFuture(expectedResult));

        CompletableFuture<LoginResult> future = useCase.execute(refreshToken);
        LoginResult actualResult = future.get();

        assertEquals(expectedResult, actualResult);
        assertEquals("new_access", actualResult.accessToken());
        assertEquals("new_refresh", actualResult.refreshToken());
    }

    @Test(expected = ExecutionException.class)
    public void execute_failure_throwsException() throws Exception {
        String refreshToken = "invalid_token";
        CompletableFuture<LoginResult> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Token expired"));
        when(authRepository.refreshToken(refreshToken)).thenReturn(failedFuture);

        CompletableFuture<LoginResult> future = useCase.execute(refreshToken);
        future.get(); // Should throw ExecutionException
    }
}
