package io.github.phunguy65.zms.presentation.main.accountsettings;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.net.Uri;
import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.domain.model.User;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.me.DeleteAccountUseCase;
import io.github.phunguy65.zms.domain.usecase.me.GetMeUseCase;
import io.github.phunguy65.zms.domain.usecase.me.UpdateProfileUseCase;
import io.github.phunguy65.zms.presentation.main.accountsettings.AccountSettingsViewModel.AccountSettingsUiState;
import io.github.phunguy65.zms.presentation.main.accountsettings.AccountSettingsViewModel.SaveEvent;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link AccountSettingsViewModel}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountSettingsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private GetMeUseCase getMeUseCase;

    @Mock
    private UpdateProfileUseCase updateProfileUseCase;

    @Mock
    private DeleteAccountUseCase deleteAccountUseCase;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private Uri mockAvatarUri;

    // Immediate executor for testing
    private final Executor testExecutor = Runnable::run;

    private AccountSettingsViewModel viewModel;

    private static final String USER_ID = "user123";
    private static final String EMAIL = "test@example.com";
    private static final String FULL_NAME = "John Doe";
    private static final String USERNAME = "johndoe";
    private static final String AVATAR_URL = "https://avatar.url/user.jpg";
    private static final String MOCK_AVATAR_URI_STRING = "content://mock/avatar/uri";

    @Before
    public void setup() {
        // Default: getMeUseCase returns a successful user
        User defaultUser = new User(USER_ID, EMAIL, FULL_NAME, USERNAME, AVATAR_URL);
        when(getMeUseCase.execute()).thenReturn(CompletableFuture.completedFuture(defaultUser));
        when(mockAvatarUri.toString()).thenReturn(MOCK_AVATAR_URI_STRING);

        viewModel = new AccountSettingsViewModel(
                getMeUseCase, updateProfileUseCase, deleteAccountUseCase,
                sessionRepository, testExecutor);
    }

    // ==================== Initial Load Tests ====================

    @Test
    public void loadProfile_success_emitsContentState() {
        AccountSettingsUiState state = viewModel.getUiState().getValue();

        assertTrue(state instanceof AccountSettingsUiState.Content);
        AccountSettingsUiState.Content content = (AccountSettingsUiState.Content) state;
        assertEquals(AVATAR_URL, content.avatarUrl());
        assertEquals(FULL_NAME, content.fullName());
        assertEquals(USERNAME, content.username());
        assertFalse(content.hasChanges());
        assertFalse(content.isSaving());
    }

    @Test
    public void loadProfile_failure_emitsErrorState() {
        // Arrange
        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Network error"));
        when(getMeUseCase.execute()).thenReturn(failedFuture);

        // Create new ViewModel
        viewModel = new AccountSettingsViewModel(
                getMeUseCase, updateProfileUseCase, deleteAccountUseCase,
                sessionRepository, testExecutor);

        // Assert
        AccountSettingsUiState state = viewModel.getUiState().getValue();
        assertTrue(state instanceof AccountSettingsUiState.Error);
        AccountSettingsUiState.Error error = (AccountSettingsUiState.Error) state;
        assertEquals("Network error", error.message());
    }

    // ==================== Form Change Tests ====================

    @Test
    public void setFullName_updatesFormAndDetectsChange() {
        viewModel.setFullName("New Name");

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals("New Name", content.fullName());
        assertTrue(content.hasChanges());
    }

    @Test
    public void setFullName_sameAsOriginal_noChangesDetected() {
        viewModel.setFullName("Different Name");
        viewModel.setFullName(FULL_NAME); // Back to original

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertFalse(content.hasChanges());
    }

    @Test
    public void setUsername_updatesFormAndDetectsChange() {
        viewModel.setUsername("newusername");

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals("newusername", content.username());
        assertTrue(content.hasChanges());
    }

    @Test
    public void setNewAvatarUri_detectsChange() {
        viewModel.setNewAvatarUri(mockAvatarUri);

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals(mockAvatarUri, content.pendingAvatarUri());
        assertFalse(content.avatarRemoved());
        assertTrue(content.hasChanges());
    }

    @Test
    public void removeAvatar_detectsChange() {
        viewModel.removeAvatar();

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertTrue(content.avatarRemoved());
        assertNull(content.pendingAvatarUri());
        assertTrue(content.hasChanges());
    }

    @Test
    public void setNewAvatarUri_afterRemove_clearsRemoveFlag() {
        viewModel.removeAvatar();
        viewModel.setNewAvatarUri(mockAvatarUri);

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertFalse(content.avatarRemoved());
        assertEquals(mockAvatarUri, content.pendingAvatarUri());
    }

    // ==================== Validation Tests ====================

    @Test
    public void setFullName_empty_showsError() {
        viewModel.setFullName("");

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals("Full name is required", content.fullNameError());
        assertFalse(content.hasChanges()); // hasChanges should account for validation
    }

    @Test
    public void setFullName_tooLong_showsError() {
        String longName = "a".repeat(256);
        viewModel.setFullName(longName);

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals("Full name is too long (max 255 characters)", content.fullNameError());
    }

    @Test
    public void setUsername_tooShort_showsError() {
        viewModel.setUsername("ab"); // 2 characters

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals("Username must be at least 3 characters", content.usernameError());
    }

    @Test
    public void setUsername_tooLong_showsError() {
        String longUsername = "a".repeat(31);
        viewModel.setUsername(longUsername);

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals("Username must be at most 30 characters", content.usernameError());
    }

    @Test
    public void setUsername_invalidCharacters_showsError() {
        viewModel.setUsername("user@name!");

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals(
                "Username can only contain letters, numbers, _ and -", content.usernameError());
    }

    @Test
    public void setUsername_validCharacters_noError() {
        viewModel.setUsername("user_name-123");

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertNull(content.usernameError());
    }

    @Test
    public void setUsername_empty_showsError() {
        // With PUT semantics, username is now required
        viewModel.setUsername("");

        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals("Username is required", content.usernameError());
    }

    // ==================== hasChanges Tests ====================

    @Test
    public void hasChanges_noChanges_returnsFalse() {
        assertFalse(viewModel.hasChanges());
    }

    @Test
    public void hasChanges_fullNameChanged_returnsTrue() {
        viewModel.setFullName("New Name");
        assertTrue(viewModel.hasChanges());
    }

    @Test
    public void hasChanges_usernameChanged_returnsTrue() {
        viewModel.setUsername("newuser");
        assertTrue(viewModel.hasChanges());
    }

    @Test
    public void hasChanges_avatarChanged_returnsTrue() {
        viewModel.setNewAvatarUri(mockAvatarUri);
        assertTrue(viewModel.hasChanges());
    }

    @Test
    public void hasChanges_avatarRemoved_returnsTrue() {
        viewModel.removeAvatar();
        assertTrue(viewModel.hasChanges());
    }

    // ==================== Save Tests ====================

    @Test
    public void saveProfile_withValidationErrors_doesNotCallUseCase() {
        viewModel.setFullName(""); // Invalid

        viewModel.saveProfile();

        verify(updateProfileUseCase, never())
                .execute(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    public void saveProfile_success_callsUpdateProfileUseCaseWithAllFields() {
        // Arrange
        viewModel.setFullName("New Name");
        User updatedUser = new User(USER_ID, EMAIL, "New Name", USERNAME, AVATAR_URL);
        // With PUT semantics, ALL fields are sent
        when(updateProfileUseCase.execute(
                        eq(USER_ID),
                        eq("New Name"),
                        eq(USERNAME),
                        eq(AVATAR_URL),
                        isNull(),
                        eq(false)))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        viewModel.saveProfile();

        // Assert - all fields are sent, not just changed ones
        verify(updateProfileUseCase)
                .execute(USER_ID, "New Name", USERNAME, AVATAR_URL, null, false);
    }

    @Test
    public void saveProfile_success_updatesSessionRepository() {
        // Arrange
        viewModel.setFullName("New Name");
        User updatedUser = new User(USER_ID, EMAIL, "New Name", USERNAME, AVATAR_URL);
        when(updateProfileUseCase.execute(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        viewModel.saveProfile();

        // Assert
        verify(sessionRepository).updateUserProfile("New Name", USERNAME, AVATAR_URL);
    }

    @Test
    public void saveProfile_success_emitsSuccessEvent() {
        // Arrange
        viewModel.setFullName("New Name");
        User updatedUser = new User(USER_ID, EMAIL, "New Name", USERNAME, AVATAR_URL);
        when(updateProfileUseCase.execute(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        viewModel.saveProfile();

        // Assert
        SaveEvent event = viewModel.getSaveEvent().getValue();
        assertTrue(event instanceof SaveEvent.Success);
    }

    @Test
    public void saveProfile_usernameConflict_showsInlineError() {
        // Arrange
        viewModel.setUsername("takenusername");
        ApiFailException conflict = new ApiFailException(
                "USERNAME_ALREADY_EXISTS", "Username taken", Collections.emptyList());
        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(conflict);
        when(updateProfileUseCase.execute(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(failedFuture);

        // Act
        viewModel.saveProfile();

        // Assert
        AccountSettingsUiState.Content content =
                (AccountSettingsUiState.Content) viewModel.getUiState().getValue();
        assertEquals("Username is already taken", content.usernameError());
        assertFalse(content.isSaving());
    }

    @Test
    public void saveProfile_generalError_emitsErrorEvent() {
        // Arrange
        viewModel.setFullName("New Name");
        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Server error"));
        when(updateProfileUseCase.execute(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(failedFuture);

        // Act
        viewModel.saveProfile();

        // Assert
        SaveEvent event = viewModel.getSaveEvent().getValue();
        assertTrue(event instanceof SaveEvent.Error);
        assertEquals("Server error", ((SaveEvent.Error) event).message());
    }

    @Test
    public void saveProfile_alwaysSendsAllFields() {
        // Change only username
        viewModel.setUsername("newuser");

        User updatedUser = new User(USER_ID, EMAIL, FULL_NAME, "newuser", AVATAR_URL);
        // With PUT semantics, ALL fields are always sent
        when(updateProfileUseCase.execute(
                        eq(USER_ID),
                        eq(FULL_NAME),
                        eq("newuser"),
                        eq(AVATAR_URL),
                        isNull(),
                        eq(false)))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        viewModel.saveProfile();

        // All fields sent, not just changed ones
        verify(updateProfileUseCase)
                .execute(USER_ID, FULL_NAME, "newuser", AVATAR_URL, null, false);
    }

    @Test
    public void saveProfile_withNewAvatar_sendsAvatarUriAsString() {
        // Arrange
        viewModel.setNewAvatarUri(mockAvatarUri);
        User updatedUser = new User(USER_ID, EMAIL, FULL_NAME, USERNAME, "https://new.avatar.url");
        // ViewModel converts Uri to String when calling use case
        when(updateProfileUseCase.execute(
                        eq(USER_ID),
                        eq(FULL_NAME),
                        eq(USERNAME),
                        eq(AVATAR_URL),
                        eq(MOCK_AVATAR_URI_STRING),
                        eq(false)))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        viewModel.saveProfile();

        // Assert - Uri is converted to String when passed to use case
        verify(updateProfileUseCase)
                .execute(USER_ID, FULL_NAME, USERNAME, AVATAR_URL, MOCK_AVATAR_URI_STRING, false);
    }

    @Test
    public void saveProfile_withAvatarRemoval_sendsNullCurrentAvatarAndRemoveTrue() {
        // Arrange
        viewModel.removeAvatar();
        User updatedUser = new User(USER_ID, EMAIL, FULL_NAME, USERNAME, null);
        // When removing avatar: currentAvatarUrl is null (since avatarRemoved is true),
        // removeAvatar is true
        when(updateProfileUseCase.execute(
                        eq(USER_ID), eq(FULL_NAME), eq(USERNAME), isNull(), isNull(), eq(true)))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        viewModel.saveProfile();

        // Assert
        verify(updateProfileUseCase).execute(USER_ID, FULL_NAME, USERNAME, null, null, true);
    }

    @Test
    public void saveProfile_success_resetsHasChanges() {
        // Arrange
        viewModel.setFullName("New Name");
        User updatedUser = new User(USER_ID, EMAIL, "New Name", USERNAME, AVATAR_URL);
        when(updateProfileUseCase.execute(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        viewModel.saveProfile();

        // Assert
        assertFalse(viewModel.hasChanges());
    }

    // ==================== SaveEvent Tests ====================

    @Test
    public void saveEvent_success_createsSuccessEvent() {
        SaveEvent event = SaveEvent.success();
        assertTrue(event instanceof SaveEvent.Success);
    }

    @Test
    public void saveEvent_error_createsErrorEvent() {
        SaveEvent event = SaveEvent.error("Test error");
        assertTrue(event instanceof SaveEvent.Error);
        assertEquals("Test error", ((SaveEvent.Error) event).message());
    }

    // ==================== AccountSettingsUiState Tests ====================

    @Test
    public void uiState_loading_createsLoadingState() {
        AccountSettingsUiState state = AccountSettingsUiState.loading();
        assertTrue(state instanceof AccountSettingsUiState.Loading);
    }

    @Test
    public void uiState_error_createsErrorState() {
        AccountSettingsUiState state = AccountSettingsUiState.error("Error message");
        assertTrue(state instanceof AccountSettingsUiState.Error);
        assertEquals("Error message", ((AccountSettingsUiState.Error) state).message());
    }

    // ==================== Delete Account Tests ====================

    @Test
    public void requestDeleteAccount_emitsConfirmingState() {
        viewModel.requestDeleteAccount();

        AccountSettingsViewModel.DeleteUiState state = viewModel.getDeleteUiState().getValue();
        assertTrue(state instanceof AccountSettingsViewModel.DeleteUiState.Confirming);
    }

    @Test
    public void cancelDelete_resetsToIdleState() {
        viewModel.requestDeleteAccount();
        viewModel.cancelDelete();

        AccountSettingsViewModel.DeleteUiState state = viewModel.getDeleteUiState().getValue();
        assertTrue(state instanceof AccountSettingsViewModel.DeleteUiState.Idle);
    }

    @Test
    public void confirmDeleteAccount_wrongText_doesNothing() {
        viewModel.requestDeleteAccount();
        viewModel.confirmDeleteAccount("WRONG");

        AccountSettingsViewModel.DeleteUiState state = viewModel.getDeleteUiState().getValue();
        assertTrue(state instanceof AccountSettingsViewModel.DeleteUiState.Confirming);
        verify(deleteAccountUseCase, never()).execute();
    }

    @Test
    public void confirmDeleteAccount_correctText_transitionsToDeletedOnSuccess() {
        when(deleteAccountUseCase.execute())
                .thenReturn(CompletableFuture.completedFuture(null));

        viewModel.requestDeleteAccount();
        viewModel.confirmDeleteAccount("DELETE");

        AccountSettingsViewModel.DeleteUiState state = viewModel.getDeleteUiState().getValue();
        assertTrue(state instanceof AccountSettingsViewModel.DeleteUiState.Deleted);
    }

    @Test
    public void confirmDeleteAccount_correctText_transitionsToErrorOnFailure() {
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Delete failed"));
        when(deleteAccountUseCase.execute()).thenReturn(failedFuture);

        viewModel.requestDeleteAccount();
        viewModel.confirmDeleteAccount("DELETE");

        AccountSettingsViewModel.DeleteUiState state = viewModel.getDeleteUiState().getValue();
        assertTrue(state instanceof AccountSettingsViewModel.DeleteUiState.Error);
        assertEquals("Delete failed",
                ((AccountSettingsViewModel.DeleteUiState.Error) state).message());
    }

    @Test
    public void confirmDeleteAccount_lowercaseDelete_doesNothing() {
        viewModel.requestDeleteAccount();
        viewModel.confirmDeleteAccount("delete");

        AccountSettingsViewModel.DeleteUiState state = viewModel.getDeleteUiState().getValue();
        assertTrue(state instanceof AccountSettingsViewModel.DeleteUiState.Confirming);
        verify(deleteAccountUseCase, never()).execute();
    }
}
