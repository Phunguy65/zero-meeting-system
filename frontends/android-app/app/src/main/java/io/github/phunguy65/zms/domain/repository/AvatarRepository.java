package io.github.phunguy65.zms.domain.repository;

import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for avatar storage operations.
 *
 * <p>This abstraction allows the domain layer to interact with avatar storage
 * without knowing the underlying implementation (Firebase Storage, S3, etc.).
 *
 * <p>Note: This interface uses String for image paths to keep the domain layer
 * platform-agnostic. The implementation converts these to platform-specific
 * types (e.g., android.net.Uri) as needed.
 */
public interface AvatarRepository {

    /**
     * Uploads an avatar image for the specified user.
     *
     * @param userId the user's ID (used to identify the avatar)
     * @param imageUriString URI string of the local image to upload (e.g., "content://..." or "file://...")
     * @return a future that completes with the public download URL
     */
    CompletableFuture<String> uploadAvatar(String userId, String imageUriString);

    /**
     * Deletes the avatar image for the specified user.
     *
     * @param userId the user's ID
     * @return a future that completes when deletion is done
     */
    CompletableFuture<Void> deleteAvatar(String userId);
}
