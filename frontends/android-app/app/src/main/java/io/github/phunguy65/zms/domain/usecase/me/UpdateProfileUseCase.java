package io.github.phunguy65.zms.domain.usecase.me;

import io.github.phunguy65.zms.domain.model.User;
import io.github.phunguy65.zms.domain.repository.AvatarRepository;
import io.github.phunguy65.zms.domain.repository.MeRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for updating the current authenticated user's profile.
 *
 * <p>Orchestrates avatar upload and profile update to backend API.
 * If a new avatar image is provided, it is uploaded first, then the profile update includes
 * the new avatar URL.
 *
 * <p>Flow:
 * <ol>
 *   <li>If new avatar image provided: upload via AvatarRepository → get public URL</li>
 *   <li>PUT /api/v1/me with all fields (including avatarUrl)</li>
 *   <li>Return updated user profile</li>
 * </ol>
 *
 * <p>All parameters are required with PUT semantics. Use {@code null} for {@code avatarUrl}
 * to clear the avatar. Use the current avatar URL to keep it unchanged.
 *
 * <p>Note: This use case is platform-agnostic. Image URIs are passed as strings
 * and converted to platform-specific types by the repository implementation.
 */
public class UpdateProfileUseCase {

    private final MeRepository meRepository;
    private final AvatarRepository avatarRepository;

    @Inject
    public UpdateProfileUseCase(MeRepository meRepository, AvatarRepository avatarRepository) {
        this.meRepository = meRepository;
        this.avatarRepository = avatarRepository;
    }

    /**
     * Executes the profile update operation.
     *
     * @param userId the current user's ID (used for storage path)
     * @param fullName the full name (required)
     * @param username the username (required)
     * @param currentAvatarUrl the current avatar URL (to preserve if not changing)
     * @param newAvatarUriString URI string of new avatar image, or null if not uploading new avatar
     * @param removeAvatar if true, removes the avatar (passes null to API)
     * @return a future that completes with the updated user profile
     */
    public CompletableFuture<User> execute(
            String userId,
            String fullName,
            String username,
            String currentAvatarUrl,
            String newAvatarUriString,
            boolean removeAvatar) {

        if (newAvatarUriString != null) {
            return avatarRepository
                    .uploadAvatar(userId, newAvatarUriString)
                    .thenCompose(avatarUrl -> meRepository.updateMe(fullName, username, avatarUrl));
        } else if (removeAvatar) {
            return avatarRepository
                    .deleteAvatar(userId)
                    .exceptionally(e -> null)
                    .thenCompose(ignored -> meRepository.updateMe(fullName, username, null));
        } else {
            return meRepository.updateMe(fullName, username, currentAvatarUrl);
        }
    }
}
