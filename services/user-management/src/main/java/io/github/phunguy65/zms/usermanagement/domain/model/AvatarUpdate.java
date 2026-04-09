package io.github.phunguy65.zms.usermanagement.domain.model;

/**
 * Discriminated union encoding the caller's intent for the {@code avatarUrl} field
 * in a profile update operation.
 *
 * <ul>
 *   <li>{@link Keep} — field was absent in the PATCH body; leave current value unchanged.</li>
 *   <li>{@link Set} — field was present with a non-null value; replace with the new URL.</li>
 *   <li>{@link Clear} — field was explicitly set to {@code null}; remove the avatar.</li>
 * </ul>
 */
public sealed interface AvatarUpdate {

    /** Leave the current avatar URL unchanged. */
    record Keep() implements AvatarUpdate {}

    /** Replace the avatar URL with a new value. */
    record Set(String url) implements AvatarUpdate {}

    /** Remove the avatar URL (set to {@code null}). */
    record Clear() implements AvatarUpdate {}
}
