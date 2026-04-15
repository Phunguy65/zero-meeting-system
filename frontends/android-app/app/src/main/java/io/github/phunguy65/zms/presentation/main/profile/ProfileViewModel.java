package io.github.phunguy65.zms.presentation.main.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * ViewModel for the profile screen.
 *
 * <p>Handles logout by clearing tokens and session data.
 */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final SessionRepository sessionRepository;
    private final Executor ioExecutor;
    private final Executor mainExecutor;

    private final MutableLiveData<Boolean> logoutComplete = new MutableLiveData<>(false);

    @Inject
    public ProfileViewModel(
            SessionRepository sessionRepository,
            @IoExecutor Executor ioExecutor,
            @MainExecutor Executor mainExecutor) {
        this.sessionRepository = sessionRepository;
        this.ioExecutor = ioExecutor;
        this.mainExecutor = mainExecutor;
    }

    /**
     * Returns LiveData that emits true when logout is complete.
     * Observe this to navigate to WelcomeActivity.
     */
    public LiveData<Boolean> getLogoutComplete() {
        return logoutComplete;
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
        CompletableFuture.runAsync(
                        sessionRepository::clearAllSessionData,
                        ioExecutor)
                .thenRunAsync(() -> logoutComplete.setValue(true), mainExecutor);
    }
}
