package io.github.phunguy65.zms.domain.model;

/**
 * Result of an avatar upload operation.
 *
 * <p>On success, holds the public URL of the uploaded avatar.
 * On failure, holds the error that occurred.
 */
public record AvatarUploadResult(String url, Throwable error) {
    /**
     * Creates a successful result with the avatar URL.
     *
     * @param url the public URL of the uploaded avatar
     * @return a successful result
     */
    public static AvatarUploadResult success(String url) {
        return new AvatarUploadResult(url, null);
    }

    /**
     * Creates a failed result with the error.
     *
     * @param error the error that occurred during upload
     * @return a failed result
     */
    public static AvatarUploadResult failure(Throwable error) {
        return new AvatarUploadResult(null, error);
    }

    /**
     * Checks if the upload was successful.
     *
     * @return true if successful, false if failed
     */
    public boolean isSuccess() {
        return error == null && url != null;
    }
}
