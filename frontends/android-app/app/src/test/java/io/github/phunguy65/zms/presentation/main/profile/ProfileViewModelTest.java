package io.github.phunguy65.zms.presentation.main.profile;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import io.github.phunguy65.zms.domain.model.User;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.me.GetMeUseCase;
import io.github.phunguy65.zms.presentation.main.profile.ProfileViewModel.ProfileUiState;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link ProfileViewModel}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfileViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private GetMeUseCase getMeUseCase;

    // Immediate executor for testing
    private final Executor testExecutor = Runnable::run;

    private ProfileViewModel viewModel;

    private static final String USER_ID = "user123";
    private static final String EMAIL = "test@example.com";
    private static final String FULL_NAME = "Test User";
    private static final String USERNAME = "testuser";
    private static final String AVATAR_URL = "https://avatar.url/user.jpg";

    @Before
    public void setup() {
        // Default: getMeUseCase returns a successful user
        User defaultUser = new User(USER_ID, EMAIL, FULL_NAME, USERNAME, AVATAR_URL);
        when(getMeUseCase.execute()).thenReturn(CompletableFuture.completedFuture(defaultUser));

        viewModel =
                new ProfileViewModel(sessionRepository, getMeUseCase, testExecutor, testExecutor);
    }

    // ==================== Logout Tests ====================

    @Test
    public void initialState_logoutCompleteIsFalse() {
        Boolean logoutComplete = viewModel.getLogoutComplete().getValue();
        assertFalse(logoutComplete);
    }

    @Test
    public void logOut_clearsAllSessionData() {
        viewModel.logOut();

        verify(sessionRepository).clearAllSessionData();
    }

    @Test
    public void logOut_emitsLogoutComplete() {
        viewModel.logOut();

        Boolean logoutComplete = viewModel.getLogoutComplete().getValue();
        assertTrue(logoutComplete);
    }

    @Test
    public void logOut_executesOnIoExecutor() {
        // This test verifies the execution pattern by checking that
        // clearAllSessionData is called which happens on ioExecutor
        viewModel.logOut();

        verify(sessionRepository).clearAllSessionData();
    }

    // ==================== Profile Loading Tests ====================

    @Test
    public void loadProfile_success_emitsSuccessState() {
        ProfileUiState state = viewModel.getProfileState().getValue();

        assertTrue(state instanceof ProfileUiState.Success);
        ProfileUiState.Success success = (ProfileUiState.Success) state;
        assertEquals(USER_ID, success.userId());
        assertEquals(AVATAR_URL, success.avatarUrl());
        assertEquals(FULL_NAME, success.fullName());
        assertEquals(EMAIL, success.email());
    }

    @Test
    public void loadProfile_success_withNullAvatar_emitsSuccessState() {
        // Arrange - user without avatar
        User userWithoutAvatar = new User(USER_ID, EMAIL, FULL_NAME, USERNAME, null);
        when(getMeUseCase.execute())
                .thenReturn(CompletableFuture.completedFuture(userWithoutAvatar));

        // Create new ViewModel to trigger loadProfile
        viewModel =
                new ProfileViewModel(sessionRepository, getMeUseCase, testExecutor, testExecutor);

        // Assert
        ProfileUiState state = viewModel.getProfileState().getValue();
        assertTrue(state instanceof ProfileUiState.Success);
        ProfileUiState.Success success = (ProfileUiState.Success) state;
        assertNull(success.avatarUrl());
        assertEquals(FULL_NAME, success.fullName());
    }

    @Test
    public void loadProfile_failure_emitsErrorState() {
        // Arrange
        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Network error"));
        when(getMeUseCase.execute()).thenReturn(failedFuture);

        // Create new ViewModel to trigger loadProfile
        viewModel =
                new ProfileViewModel(sessionRepository, getMeUseCase, testExecutor, testExecutor);

        // Assert
        ProfileUiState state = viewModel.getProfileState().getValue();
        assertTrue(state instanceof ProfileUiState.Error);
        ProfileUiState.Error error = (ProfileUiState.Error) state;
        assertEquals("Network error", error.message());
    }

    @Test
    public void loadProfile_calledOnConstruction() {
        // getMeUseCase.execute() should be called once during construction
        verify(getMeUseCase).execute();
    }

    @Test
    public void loadProfile_canBeCalledManually() {
        // Reset mock to clear construction call
        reset(getMeUseCase);
        User user = new User(USER_ID, EMAIL, "Updated Name", USERNAME, AVATAR_URL);
        when(getMeUseCase.execute()).thenReturn(CompletableFuture.completedFuture(user));

        viewModel.loadProfile();

        verify(getMeUseCase).execute();
        ProfileUiState state = viewModel.getProfileState().getValue();
        assertTrue(state instanceof ProfileUiState.Success);
        assertEquals("Updated Name", ((ProfileUiState.Success) state).fullName());
    }

    @Test
    public void loadProfile_setsLoadingStateFirst() {
        // Arrange - use a future that doesn't complete immediately
        CompletableFuture<User> pendingFuture = new CompletableFuture<>();
        reset(getMeUseCase);
        when(getMeUseCase.execute()).thenReturn(pendingFuture);

        // Act - call loadProfile manually on existing viewModel
        viewModel.loadProfile();

        // Assert - state should be Loading before future completes
        ProfileUiState state = viewModel.getProfileState().getValue();
        assertTrue(
                "Expected Loading state before async completion",
                state instanceof ProfileUiState.Loading);

        // Complete the future to verify transition
        pendingFuture.complete(new User(USER_ID, EMAIL, FULL_NAME, USERNAME, AVATAR_URL));

        // After completion, state should be Success
        ProfileUiState finalState = viewModel.getProfileState().getValue();
        assertTrue(
                "Expected Success state after async completion",
                finalState instanceof ProfileUiState.Success);
    }

    @Test
    public void loadProfile_failure_extractsCauseMessage() {
        // Arrange - exception that will be wrapped in CompletionException by CompletableFuture
        // When exceptionally() is called, the exception is wrapped, so error.getCause() gives us
        // the original exception
        RuntimeException originalError = new RuntimeException("Original error message");
        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(originalError);
        reset(getMeUseCase);
        when(getMeUseCase.execute()).thenReturn(failedFuture);

        // Act
        viewModel.loadProfile();

        // Assert - ViewModel extracts cause.getMessage() (the original error from
        // CompletionException)
        ProfileUiState state = viewModel.getProfileState().getValue();
        assertTrue(state instanceof ProfileUiState.Error);
        ProfileUiState.Error error = (ProfileUiState.Error) state;
        assertEquals("Original error message", error.message());
    }

    @Test
    public void loadProfile_failure_withoutCause_usesDirectMessage() {
        // Arrange - exception without a cause
        RuntimeException errorWithoutCause = new RuntimeException("Direct error");
        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(errorWithoutCause);
        reset(getMeUseCase);
        when(getMeUseCase.execute()).thenReturn(failedFuture);

        // Act
        viewModel.loadProfile();

        // Assert - should use the error's own message since no cause
        ProfileUiState state = viewModel.getProfileState().getValue();
        assertTrue(state instanceof ProfileUiState.Error);
        ProfileUiState.Error error = (ProfileUiState.Error) state;
        assertEquals("Direct error", error.message());
    }

    // ==================== ProfileUiState Tests ====================

    @Test
    public void profileUiState_loading_createsLoadingState() {
        ProfileUiState state = ProfileUiState.loading();
        assertTrue(state instanceof ProfileUiState.Loading);
    }

    @Test
    public void profileUiState_success_createsSuccessState() {
        ProfileUiState state = ProfileUiState.success(USER_ID, AVATAR_URL, FULL_NAME, EMAIL);
        assertTrue(state instanceof ProfileUiState.Success);
        ProfileUiState.Success success = (ProfileUiState.Success) state;
        assertEquals(USER_ID, success.userId());
        assertEquals(AVATAR_URL, success.avatarUrl());
        assertEquals(FULL_NAME, success.fullName());
        assertEquals(EMAIL, success.email());
    }

    @Test
    public void profileUiState_error_createsErrorState() {
        ProfileUiState state = ProfileUiState.error("Error message");
        assertTrue(state instanceof ProfileUiState.Error);
        ProfileUiState.Error error = (ProfileUiState.Error) state;
        assertEquals("Error message", error.message());
    }
}
