package io.github.phunguy65.zms.presentation.main.accountsettings;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.me.DeleteAccountUseCase;
import io.github.phunguy65.zms.domain.usecase.me.GetMeUseCase;
import io.github.phunguy65.zms.domain.usecase.me.UpdateProfileUseCase;
import io.github.phunguy65.zms.presentation.common.util.SingleLiveEvent;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import javax.inject.Inject;

/**
 * ViewModel for the Account Settings screen.
 *
 * <p>Manages form state, validation, avatar changes, and profile update operations.
 */
@HiltViewModel
public class AccountSettingsViewModel extends ViewModel {

    private static final int FULLNAME_MAX_LENGTH = 255;
    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 30;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private static final String DELETE_CONFIRMATION_TEXT = "DELETE";

    private final GetMeUseCase getMeUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final SessionRepository sessionRepository;
    private final Executor mainExecutor;

    private final MutableLiveData<AccountSettingsUiState> uiState =
            new MutableLiveData<>(AccountSettingsUiState.loading());
    private final SingleLiveEvent<SaveEvent> saveEvent = new SingleLiveEvent<>();
    private final MutableLiveData<DeleteUiState> deleteUiState =
            new MutableLiveData<>(new DeleteUiState.Idle());

    private String originalFullName;
    private String originalUsername;
    private String originalAvatarUrl;
    private String userId;

    private String currentFullName = "";
    private String currentUsername = "";
    private Uri pendingAvatarUri = null;
    private boolean avatarRemoved = false;

    @Inject
    public AccountSettingsViewModel(
            GetMeUseCase getMeUseCase,
            UpdateProfileUseCase updateProfileUseCase,
            DeleteAccountUseCase deleteAccountUseCase,
            SessionRepository sessionRepository,
            @MainExecutor Executor mainExecutor) {
        this.getMeUseCase = getMeUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.deleteAccountUseCase = deleteAccountUseCase;
        this.sessionRepository = sessionRepository;
        this.mainExecutor = mainExecutor;

        loadProfile();
    }

    public LiveData<AccountSettingsUiState> getUiState() {
        return uiState;
    }

    public LiveData<SaveEvent> getSaveEvent() {
        return saveEvent;
    }

    /**
     * Returns the LiveData for delete-account UI state.
     */
    public LiveData<DeleteUiState> getDeleteUiState() {
        return deleteUiState;
    }

    /**
     * Initiates the delete-account flow by transitioning to the confirming state.
     */
    public void requestDeleteAccount() {
        deleteUiState.setValue(new DeleteUiState.Confirming());
    }

    /**
     * Cancels the delete-account flow and returns to idle.
     */
    public void cancelDelete() {
        deleteUiState.setValue(new DeleteUiState.Idle());
    }

    /**
     * Confirms account deletion if the confirmation text matches exactly {@code DELETE}.
     *
     * @param confirmText the user-typed confirmation string
     */
    public void confirmDeleteAccount(String confirmText) {
        if (!DELETE_CONFIRMATION_TEXT.equals(confirmText)) {
            return;
        }

        deleteUiState.setValue(new DeleteUiState.Deleting());

        deleteAccountUseCase
                .execute()
                .thenRunAsync(
                        () -> deleteUiState.setValue(new DeleteUiState.Deleted()), mainExecutor)
                .exceptionally(error -> {
                    mainExecutor.execute(() -> {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        deleteUiState.setValue(new DeleteUiState.Error(cause.getMessage()));
                    });
                    return null;
                });
    }

    /**
     * Loads the current user's profile from the API.
     */
    public void loadProfile() {
        uiState.setValue(AccountSettingsUiState.loading());

        getMeUseCase
                .execute()
                .thenAcceptAsync(
                        user -> {
                            userId = user.id();
                            originalFullName = user.fullName() != null ? user.fullName() : "";
                            originalUsername = user.username() != null ? user.username() : "";
                            originalAvatarUrl = user.avatarUrl();

                            currentFullName = originalFullName;
                            currentUsername = originalUsername;
                            pendingAvatarUri = null;
                            avatarRemoved = false;

                            emitContentState();
                        },
                        mainExecutor)
                .exceptionally(error -> {
                    mainExecutor.execute(() -> uiState.setValue(AccountSettingsUiState.error(
                            error.getCause() != null
                                    ? error.getCause().getMessage()
                                    : error.getMessage())));
                    return null;
                });
    }

    /**
     * Updates the full name field value.
     */
    public void setFullName(String fullName) {
        currentFullName = fullName != null ? fullName : "";
        emitContentState();
    }

    /**
     * Updates the username field value.
     */
    public void setUsername(String username) {
        currentUsername = username != null ? username : "";
        emitContentState();
    }

    /**
     * Sets a new avatar image URI.
     */
    public void setNewAvatarUri(Uri uri) {
        pendingAvatarUri = uri;
        avatarRemoved = false;
        emitContentState();
    }

    /**
     * Marks the avatar for removal.
     */
    public void removeAvatar() {
        pendingAvatarUri = null;
        avatarRemoved = true;
        emitContentState();
    }

    /**
     * Checks if there are unsaved changes.
     */
    public boolean hasChanges() {
        boolean nameChanged = !currentFullName.equals(originalFullName);
        boolean usernameChanged = !currentUsername.equals(originalUsername);
        boolean avatarChanged = pendingAvatarUri != null || avatarRemoved;
        return nameChanged || usernameChanged || avatarChanged;
    }

    /**
     * Saves the profile changes.
     */
    public void saveProfile() {
        String fullNameError = validateFullName(currentFullName);
        String usernameError = validateUsername(currentUsername);

        if (fullNameError != null || usernameError != null) {
            emitContentState();
            return;
        }

        AccountSettingsUiState current = uiState.getValue();
        if (current instanceof AccountSettingsUiState.Content content) {
            uiState.setValue(new AccountSettingsUiState.Content(
                    content.avatarUrl(),
                    content.pendingAvatarUri(),
                    content.avatarRemoved(),
                    content.fullName(),
                    content.username(),
                    content.email(),
                    null,
                    null,
                    content.hasChanges(),
                    true));
        }

        String currentAvatarUrl = avatarRemoved ? null : originalAvatarUrl;

        String pendingAvatarUriString =
                pendingAvatarUri != null ? pendingAvatarUri.toString() : null;

        updateProfileUseCase
                .execute(
                        userId,
                        currentFullName,
                        currentUsername,
                        currentAvatarUrl,
                        pendingAvatarUriString,
                        avatarRemoved)
                .thenAcceptAsync(
                        updatedUser -> {
                            sessionRepository.updateUserProfile(
                                    updatedUser.fullName(),
                                    updatedUser.username(),
                                    updatedUser.avatarUrl());

                            originalFullName =
                                    updatedUser.fullName() != null ? updatedUser.fullName() : "";
                            originalUsername =
                                    updatedUser.username() != null ? updatedUser.username() : "";
                            originalAvatarUrl = updatedUser.avatarUrl();
                            currentFullName = originalFullName;
                            currentUsername = originalUsername;
                            pendingAvatarUri = null;
                            avatarRemoved = false;

                            saveEvent.setValue(SaveEvent.success());
                        },
                        mainExecutor)
                .exceptionally(error -> {
                    mainExecutor.execute(() -> {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;

                        if (cause instanceof ApiFailException fail
                                && "USERNAME_ALREADY_EXISTS".equals(fail.getCode())) {
                            emitContentStateWithUsernameError("Username is already taken");
                        } else {
                            saveEvent.setValue(SaveEvent.error(cause.getMessage()));
                            emitContentState();
                        }
                    });
                    return null;
                });
    }

    private void emitContentState() {
        emitContentStateWithUsernameError(null);
    }

    private void emitContentStateWithUsernameError(String usernameOverrideError) {
        String fullNameError = validateFullName(currentFullName);
        String usernameError = usernameOverrideError != null
                ? usernameOverrideError
                : validateUsername(currentUsername);

        boolean hasValidationErrors = fullNameError != null || usernameError != null;
        boolean hasChanges = hasChanges();
        boolean canSave = hasChanges && !hasValidationErrors;

        String displayAvatarUrl;
        if (avatarRemoved) {
            displayAvatarUrl = null;
        } else if (pendingAvatarUri != null) {
            displayAvatarUrl = pendingAvatarUri.toString();
        } else {
            displayAvatarUrl = originalAvatarUrl;
        }

        uiState.setValue(new AccountSettingsUiState.Content(
                displayAvatarUrl,
                pendingAvatarUri,
                avatarRemoved,
                currentFullName,
                currentUsername,
                "",
                fullNameError,
                usernameError,
                canSave,
                false));
    }

    private String validateFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Full name is required";
        }
        if (fullName.length() > FULLNAME_MAX_LENGTH) {
            return "Full name is too long (max 255 characters)";
        }
        return null;
    }

    private String validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "Username is required";
        }
        if (username.length() < USERNAME_MIN_LENGTH) {
            return "Username must be at least 3 characters";
        }
        if (username.length() > USERNAME_MAX_LENGTH) {
            return "Username must be at most 30 characters";
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return "Username can only contain letters, numbers, _ and -";
        }
        return null;
    }

    /**
     * UI state for the Account Settings screen.
     */
    public sealed interface AccountSettingsUiState {

        record Loading() implements AccountSettingsUiState {}

        record Content(
                String avatarUrl,
                Uri pendingAvatarUri,
                boolean avatarRemoved,
                String fullName,
                String username,
                String email,
                String fullNameError,
                String usernameError,
                boolean hasChanges,
                boolean isSaving)
                implements AccountSettingsUiState {}

        record Error(String message) implements AccountSettingsUiState {}

        static AccountSettingsUiState loading() {
            return new Loading();
        }

        static AccountSettingsUiState error(String message) {
            return new Error(message);
        }
    }

    /**
     * One-time events from save operation.
     */
    public sealed interface SaveEvent {
        record Success() implements SaveEvent {}

        record Error(String message) implements SaveEvent {}

        static SaveEvent success() {
            return new Success();
        }

        static SaveEvent error(String message) {
            return new Error(message);
        }
    }

    /**
     * UI state for the delete-account flow.
     *
     * <p>Managed independently from {@link AccountSettingsUiState} to keep delete-account
     * state transitions isolated from profile-edit behavior.
     */
    public sealed interface DeleteUiState {

        record Idle() implements DeleteUiState {}

        record Confirming() implements DeleteUiState {}

        record Deleting() implements DeleteUiState {}

        record Error(String message) implements DeleteUiState {}

        record Deleted() implements DeleteUiState {}
    }
}
