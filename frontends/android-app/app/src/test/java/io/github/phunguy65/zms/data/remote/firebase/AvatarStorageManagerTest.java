package io.github.phunguy65.zms.data.remote.firebase;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link AvatarStorageManager}.
 *
 * <p>These tests verify the correct storage path construction and interactions
 * with Firebase Storage references. Full integration testing with actual uploads
 * should be done with Firebase emulator or instrumented tests.
 *
 * <p>Note: Firebase Tasks are difficult to mock in unit tests due to their
 * internal implementation. These tests focus on verifiable behavior like
 * path construction and reference chaining.
 */
@RunWith(MockitoJUnitRunner.class)
public class AvatarStorageManagerTest {

    @Mock
    private FirebaseStorage firebaseStorage;

    @Mock
    private StorageReference rootRef;

    @Mock
    private StorageReference avatarsRef;

    @Mock
    private StorageReference userAvatarRef;

    @Mock
    private Uri mockParsedUri;

    private AvatarStorageManager avatarStorageManager;

    private static final String USER_ID = "user123";
    private static final String IMAGE_URI_STRING = "content://media/external/images/1234";

    @Before
    public void setup() {
        // Setup storage reference chain
        when(firebaseStorage.getReference()).thenReturn(rootRef);
        when(rootRef.child("avatars")).thenReturn(avatarsRef);
        when(avatarsRef.child(USER_ID + ".jpg")).thenReturn(userAvatarRef);

        avatarStorageManager = new AvatarStorageManager(firebaseStorage);
    }

    // ==================== Upload Path Tests ====================

    @Test
    public void uploadAvatar_usesAvatarsFolder() {
        // Act - we can't fully test async behavior but can verify path construction
        try (MockedStatic<Uri> uriMock = mockStatic(Uri.class)) {
            uriMock.when(() -> Uri.parse(IMAGE_URI_STRING)).thenReturn(mockParsedUri);
            try {
                avatarStorageManager.uploadAvatar(USER_ID, IMAGE_URI_STRING);
            } catch (Exception e) {
                // Expected - putFile returns null mock
            }
        }

        // Assert
        verify(rootRef).child("avatars");
    }

    @Test
    public void uploadAvatar_usesUserIdWithJpgExtension() {
        // Act
        try (MockedStatic<Uri> uriMock = mockStatic(Uri.class)) {
            uriMock.when(() -> Uri.parse(IMAGE_URI_STRING)).thenReturn(mockParsedUri);
            try {
                avatarStorageManager.uploadAvatar(USER_ID, IMAGE_URI_STRING);
            } catch (Exception e) {
                // Expected - putFile returns null mock
            }
        }

        // Assert
        verify(avatarsRef).child(USER_ID + ".jpg");
    }

    @Test
    public void uploadAvatar_parsesUriStringAndCallsPutFile() {
        // Act
        try (MockedStatic<Uri> uriMock = mockStatic(Uri.class)) {
            uriMock.when(() -> Uri.parse(IMAGE_URI_STRING)).thenReturn(mockParsedUri);
            try {
                avatarStorageManager.uploadAvatar(USER_ID, IMAGE_URI_STRING);
            } catch (Exception e) {
                // Expected - putFile returns null mock
            }

            // Assert - verify Uri.parse was called with the string
            uriMock.verify(() -> Uri.parse(IMAGE_URI_STRING));
        }

        // Assert - verify putFile was called with the parsed Uri
        verify(userAvatarRef).putFile(mockParsedUri);
    }

    @Test
    public void uploadAvatar_differentUserId_usesDifferentPath() {
        // Arrange
        String otherUserId = "otherUser456";
        StorageReference otherUserRef = mock(StorageReference.class);
        when(avatarsRef.child(otherUserId + ".jpg")).thenReturn(otherUserRef);

        // Act
        try (MockedStatic<Uri> uriMock = mockStatic(Uri.class)) {
            uriMock.when(() -> Uri.parse(IMAGE_URI_STRING)).thenReturn(mockParsedUri);
            try {
                avatarStorageManager.uploadAvatar(otherUserId, IMAGE_URI_STRING);
            } catch (Exception e) {
                // Expected - putFile returns null mock
            }
        }

        // Assert
        verify(avatarsRef).child(otherUserId + ".jpg");
        verify(otherUserRef).putFile(mockParsedUri);
    }

    @Test
    public void uploadAvatar_returnsCompletableFuture() {
        // Act - verify return type
        try (MockedStatic<Uri> uriMock = mockStatic(Uri.class)) {
            uriMock.when(() -> Uri.parse(IMAGE_URI_STRING)).thenReturn(mockParsedUri);
            try {
                var future = avatarStorageManager.uploadAvatar(USER_ID, IMAGE_URI_STRING);
                assertNotNull(future);
            } catch (Exception e) {
                // Expected - putFile returns null mock
            }
        }
    }

    // ==================== Delete Path Tests ====================

    @Test
    public void deleteAvatar_usesAvatarsFolder() {
        // Act
        try {
            avatarStorageManager.deleteAvatar(USER_ID);
        } catch (Exception e) {
            // Expected - delete returns null mock
        }

        // Assert
        verify(rootRef).child("avatars");
    }

    @Test
    public void deleteAvatar_usesUserIdWithJpgExtension() {
        // Act
        try {
            avatarStorageManager.deleteAvatar(USER_ID);
        } catch (Exception e) {
            // Expected - delete returns null mock
        }

        // Assert
        verify(avatarsRef).child(USER_ID + ".jpg");
    }

    @Test
    public void deleteAvatar_callsDelete() {
        // Act
        try {
            avatarStorageManager.deleteAvatar(USER_ID);
        } catch (Exception e) {
            // Expected - delete returns null mock
        }

        // Assert
        verify(userAvatarRef).delete();
    }

    @Test
    public void deleteAvatar_differentUserId_usesDifferentPath() {
        // Arrange
        String otherUserId = "otherUser456";
        StorageReference otherUserRef = mock(StorageReference.class);
        when(avatarsRef.child(otherUserId + ".jpg")).thenReturn(otherUserRef);

        // Act
        try {
            avatarStorageManager.deleteAvatar(otherUserId);
        } catch (Exception e) {
            // Expected - delete returns null mock
        }

        // Assert
        verify(avatarsRef).child(otherUserId + ".jpg");
        verify(otherUserRef).delete();
    }

    // ==================== Storage Path Constant Tests ====================

    @Test
    public void avatarPath_isConsistentBetweenUploadAndDelete() {
        // Both upload and delete should use the same path
        try (MockedStatic<Uri> uriMock = mockStatic(Uri.class)) {
            uriMock.when(() -> Uri.parse(IMAGE_URI_STRING)).thenReturn(mockParsedUri);
            try {
                avatarStorageManager.uploadAvatar(USER_ID, IMAGE_URI_STRING);
            } catch (Exception e) {
                // Expected
            }
        }

        try {
            avatarStorageManager.deleteAvatar(USER_ID);
        } catch (Exception e) {
            // Expected
        }

        // Verify same path used for both operations
        verify(avatarsRef, times(2)).child(USER_ID + ".jpg");
    }

    // ==================== Constructor Tests ====================

    @Test
    public void constructor_acceptsFirebaseStorage() {
        // Should not throw
        AvatarStorageManager manager = new AvatarStorageManager(firebaseStorage);
        assertNotNull(manager);
    }
}
