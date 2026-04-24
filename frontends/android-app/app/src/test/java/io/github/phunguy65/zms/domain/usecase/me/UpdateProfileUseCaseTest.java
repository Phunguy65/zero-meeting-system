package io.github.phunguy65.zms.domain.usecase.me;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.domain.model.User;
import io.github.phunguy65.zms.domain.repository.AvatarRepository;
import io.github.phunguy65.zms.domain.repository.MeRepository;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link UpdateProfileUseCase}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateProfileUseCaseTest {

    @Mock
    private MeRepository meRepository;

    @Mock
    private AvatarRepository avatarRepository;

    private UpdateProfileUseCase useCase;

    private static final String USER_ID = "user123";
    private static final String FULL_NAME = "John Doe";
    private static final String USERNAME = "johndoe";
    private static final String CURRENT_AVATAR_URL = "https://current.com/avatar.jpg";
    private static final String UPLOADED_AVATAR_URL =
            "https://firebase.storage/avatars/user123.jpg";
    private static final String NEW_AVATAR_URI_STRING = "content://media/external/images/1234";

    @Before
    public void setup() {
        useCase = new UpdateProfileUseCase(meRepository, avatarRepository);
    }

    @Test
    public void execute_withNewAvatar_uploadsAvatarFirst() throws Exception {
        // Arrange
        User updatedUser =
                new User(USER_ID, "john@test.com", FULL_NAME, USERNAME, UPLOADED_AVATAR_URL);
        when(avatarRepository.uploadAvatar(eq(USER_ID), eq(NEW_AVATAR_URI_STRING)))
                .thenReturn(CompletableFuture.completedFuture(UPLOADED_AVATAR_URL));
        when(meRepository.updateMe(eq(FULL_NAME), eq(USERNAME), eq(UPLOADED_AVATAR_URL)))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        CompletableFuture<User> result = useCase.execute(
                USER_ID, FULL_NAME, USERNAME, CURRENT_AVATAR_URL, NEW_AVATAR_URI_STRING, false);
        User actualUser = result.get();

        // Assert
        verify(avatarRepository).uploadAvatar(USER_ID, NEW_AVATAR_URI_STRING);
        verify(meRepository).updateMe(FULL_NAME, USERNAME, UPLOADED_AVATAR_URL);
        assertEquals(updatedUser, actualUser);
    }

    @Test
    public void execute_withNewAvatar_usesUploadedUrlInUpdate() throws Exception {
        // Arrange
        String uploadedUrl = "https://newurl.com/avatar.jpg";
        User updatedUser = new User(USER_ID, "john@test.com", FULL_NAME, USERNAME, uploadedUrl);
        when(avatarRepository.uploadAvatar(eq(USER_ID), eq(NEW_AVATAR_URI_STRING)))
                .thenReturn(CompletableFuture.completedFuture(uploadedUrl));
        when(meRepository.updateMe(eq(FULL_NAME), eq(USERNAME), eq(uploadedUrl)))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        CompletableFuture<User> result = useCase.execute(
                USER_ID, FULL_NAME, USERNAME, CURRENT_AVATAR_URL, NEW_AVATAR_URI_STRING, false);
        User actualUser = result.get();

        // Assert
        assertEquals(uploadedUrl, actualUser.avatarUrl());
    }

    @Test(expected = ExecutionException.class)
    public void execute_avatarUploadFails_throwsException() throws Exception {
        // Arrange
        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Upload failed"));
        when(avatarRepository.uploadAvatar(eq(USER_ID), eq(NEW_AVATAR_URI_STRING)))
                .thenReturn(failedFuture);

        // Act
        CompletableFuture<User> result = useCase.execute(
                USER_ID, FULL_NAME, USERNAME, CURRENT_AVATAR_URL, NEW_AVATAR_URI_STRING, false);
        result.get(); // Should throw

        // Assert - verify updateMe was never called
        verify(meRepository, never()).updateMe(any(), any(), any());
    }

    @Test
    public void execute_removeAvatar_deletesFromStorageAndUpdatesWithNull() throws Exception {
        // Arrange
        User updatedUser = new User(USER_ID, "john@test.com", FULL_NAME, USERNAME, null);
        when(avatarRepository.deleteAvatar(eq(USER_ID)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(meRepository.updateMe(eq(FULL_NAME), eq(USERNAME), isNull()))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        CompletableFuture<User> result =
                useCase.execute(USER_ID, FULL_NAME, USERNAME, CURRENT_AVATAR_URL, null, true);
        User actualUser = result.get();

        // Assert
        verify(avatarRepository).deleteAvatar(USER_ID);
        verify(avatarRepository, never()).uploadAvatar(any(), any());
        verify(meRepository).updateMe(FULL_NAME, USERNAME, null);
        assertNull(actualUser.avatarUrl());
    }

    @Test
    public void execute_removeAvatar_continuesEvenIfDeleteFails() throws Exception {
        // Arrange - delete fails but update should still proceed
        CompletableFuture<Void> failedDelete = new CompletableFuture<>();
        failedDelete.completeExceptionally(new RuntimeException("Delete failed"));
        when(avatarRepository.deleteAvatar(eq(USER_ID))).thenReturn(failedDelete);

        User updatedUser = new User(USER_ID, "john@test.com", FULL_NAME, USERNAME, null);
        when(meRepository.updateMe(eq(FULL_NAME), eq(USERNAME), isNull()))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        CompletableFuture<User> result =
                useCase.execute(USER_ID, FULL_NAME, USERNAME, CURRENT_AVATAR_URL, null, true);
        User actualUser = result.get();

        // Assert - should still succeed despite delete failure
        verify(avatarRepository).deleteAvatar(USER_ID);
        verify(meRepository).updateMe(FULL_NAME, USERNAME, null);
        assertNull(actualUser.avatarUrl());
    }

    @Test
    public void execute_noAvatarChange_preservesCurrentAvatarUrl() throws Exception {
        // Arrange
        User updatedUser =
                new User(USER_ID, "john@test.com", FULL_NAME, USERNAME, CURRENT_AVATAR_URL);
        when(meRepository.updateMe(eq(FULL_NAME), eq(USERNAME), eq(CURRENT_AVATAR_URL)))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        CompletableFuture<User> result =
                useCase.execute(USER_ID, FULL_NAME, USERNAME, CURRENT_AVATAR_URL, null, false);
        User actualUser = result.get();

        // Assert
        verify(avatarRepository, never()).uploadAvatar(any(), any());
        verify(avatarRepository, never()).deleteAvatar(any());
        verify(meRepository).updateMe(FULL_NAME, USERNAME, CURRENT_AVATAR_URL);
        assertEquals(CURRENT_AVATAR_URL, actualUser.avatarUrl());
    }

    @Test
    public void execute_noAvatarChange_withNullCurrentAvatar() throws Exception {
        // Arrange - user has no avatar currently
        User updatedUser = new User(USER_ID, "john@test.com", FULL_NAME, USERNAME, null);
        when(meRepository.updateMe(eq(FULL_NAME), eq(USERNAME), isNull()))
                .thenReturn(CompletableFuture.completedFuture(updatedUser));

        // Act
        CompletableFuture<User> result =
                useCase.execute(USER_ID, FULL_NAME, USERNAME, null, null, false);
        User actualUser = result.get();

        // Assert
        verify(avatarRepository, never()).uploadAvatar(any(), any());
        verify(avatarRepository, never()).deleteAvatar(any());
        verify(meRepository).updateMe(FULL_NAME, USERNAME, null);
        assertNull(actualUser.avatarUrl());
    }

    @Test(expected = ExecutionException.class)
    public void execute_updateFails_throwsException() throws Exception {
        // Arrange
        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("API error"));
        when(meRepository.updateMe(any(), any(), any())).thenReturn(failedFuture);

        // Act
        CompletableFuture<User> result =
                useCase.execute(USER_ID, FULL_NAME, USERNAME, CURRENT_AVATAR_URL, null, false);
        result.get(); // Should throw
    }

    @Test
    public void execute_avatarUploadSucceeds_updateFails_throwsException() throws Exception {
        // Arrange
        when(avatarRepository.uploadAvatar(eq(USER_ID), eq(NEW_AVATAR_URI_STRING)))
                .thenReturn(CompletableFuture.completedFuture(UPLOADED_AVATAR_URL));

        CompletableFuture<User> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Update failed"));
        when(meRepository.updateMe(any(), any(), any())).thenReturn(failedFuture);

        // Act & Assert
        try {
            CompletableFuture<User> result = useCase.execute(
                    USER_ID, FULL_NAME, USERNAME, CURRENT_AVATAR_URL, NEW_AVATAR_URI_STRING, false);
            result.get();
            fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            assertEquals("Update failed", e.getCause().getMessage());
        }
    }
}
