package io.github.phunguy65.zms.presentation.main.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.me.GetMeUseCase;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * ViewModel for the profile screen.
 *
 * <p>Fetches and exposes the current user's profile data, and handles logout.
 */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final SessionRepository sessionRepository;
    private final GetMeUseCase getMeUseCase;
    private final Executor ioExecutor;
    private final Executor mainExecutor;

    private final MutableLiveData<ProfileUiState> profileState =
            new MutableLiveData<>(ProfileUiState.loading());
    private final MutableLiveData<Boolean> logoutComplete = new MutableLiveData<>(false);

    @Inject
    public ProfileViewModel(
            SessionRepository sessionRepository,
            GetMeUseCase getMeUseCase,
            @IoExecutor Executor ioExecutor,
            @MainExecutor Executor mainExecutor) {
        this.sessionRepository = sessionRepository;
        this.getMeUseCase = getMeUseCase;
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutor;

        loadProfile();
    }

    /**
     * Returns LiveData containing the current profile UI state.
     */
    public LiveData<ProfileUiState> getProfileState() {
        return profileState;
    }

    /**
     * Returns LiveData that emits true when logout is complete.
     * Observe this to navigate to WelcomeActivity.
     */
    public LiveData<Boolean> getLogoutComplete() {
        return logoutComplete;
    }

    /**
     * Loads the user's profile from the API.
     */
    public void loadProfile() {
        profileState.setValue(ProfileUiState.loading());

        getMeUseCase
                .execute()
                .thenAcceptAsync(
                        user -> profileState.setValue(ProfileUiState.success(
                                user.id(), user.avatarUrl(), user.fullName(), user.email())),
                        mainExecutor)
                .exceptionally(error -> {
                    mainExecutor.execute(() -> profileState.setValue(ProfileUiState.error(
                            error.getCause() != null
                                    ? error.getCause().getMessage()
                                    : error.getMessage())));
                    return null;
                });
    }

    /**
     * Logs out the user by clearing all authentication and session data.
     *
     * <p>Clears:
     * <ul>
     *   <li>Access and refresh tokens</li>
     *   <li>User session data</li>
     *   <li>RememberMe flag (set to false)</li>
     * </ul>
     */
    public void logOut() {
        CompletableFuture.runAsync(sessionRepository::clearAllSessionData, ioExecutor)
                .thenRunAsync(() -> logoutComplete.setValue(true), mainExecutor);
    }

    /**
     * Sealed interface representing the UI state for the profile screen.
     */
    public sealed interface ProfileUiState {

        /**
         * Loading state - profile data is being fetched.
         */
        record Loading() implements ProfileUiState {}

        /**
         * Success state - profile data loaded successfully.
         *
         * @param userId the user's ID
         * @param avatarUrl the user's avatar URL (may be null)
         * @param fullName the user's full name
         * @param email the user's email
         */
        record Success(String userId, String avatarUrl, String fullName, String email)
                implements ProfileUiState {}

        /**
         * Error state - failed to load profile data.
         *
         * @param message the error message
         */
        record Error(String message) implements ProfileUiState {}

        static ProfileUiState loading() {
            return new Loading();
        }

        static ProfileUiState success(
                String userId, String avatarUrl, String fullName, String email) {
            return new Success(userId, avatarUrl, fullName, email);
        }

        static ProfileUiState error(String message) {
            return new Error(message);
        }
    }
}
