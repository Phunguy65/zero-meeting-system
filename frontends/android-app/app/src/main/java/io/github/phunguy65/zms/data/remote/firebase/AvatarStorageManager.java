package io.github.phunguy65.zms.data.remote.firebase;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import io.github.phunguy65.zms.domain.repository.AvatarRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Firebase Storage implementation of {@link AvatarRepository}.
 *
 * <p>Avatars are stored at path: {@code /avatars/{userId}.jpg}
 * Each upload overwrites the previous avatar for the same user.
 */
@Singleton
public class AvatarStorageManager implements AvatarRepository {

    private static final String AVATARS_PATH = "avatars";

    private final FirebaseStorage firebaseStorage;

    @Inject
    public AvatarStorageManager(FirebaseStorage firebaseStorage) {
        this.firebaseStorage = firebaseStorage;
    }

    /**
     * Uploads an avatar image to Firebase Storage.
     *
     * <p>The image is stored at {@code /avatars/{userId}.jpg} and overwrites
     * any existing avatar for the user.
     *
     * @param userId the user's ID
     * @param imageUriString URI string of the local image to upload
     * @return a future that completes with the public download URL
     */
    @Override
    public CompletableFuture<String> uploadAvatar(String userId, String imageUriString) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Uri imageUri = Uri.parse(imageUriString);
        StorageReference avatarRef =
                firebaseStorage.getReference().child(AVATARS_PATH).child(userId + ".jpg");

        avatarRef
                .putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return avatarRef.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> future.complete(uri.toString()))
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }

    /**
     * Deletes the avatar image from Firebase Storage.
     *
     * <p>This is optional since Firebase Storage overwrites on each upload.
     * Use this to explicitly free storage when a user removes their avatar.
     *
     * @param userId the user's ID
     * @return a future that completes when deletion is done (or fails)
     */
    @Override
    public CompletableFuture<Void> deleteAvatar(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        StorageReference avatarRef =
                firebaseStorage.getReference().child(AVATARS_PATH).child(userId + ".jpg");

        avatarRef
                .delete()
                .addOnSuccessListener(unused -> future.complete(null))
                .addOnFailureListener(future::completeExceptionally);

        return future;
    }
}
