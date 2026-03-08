package io.github.phunguy65.zms.usermanagement.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.shared.domain.AggregateRoot;
import io.github.phunguy65.zms.usermanagement.domain.event.UserDeletedEvent;
import io.github.phunguy65.zms.usermanagement.domain.event.UserRegisteredEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * User aggregate root. Represents a registered account in the system.
 */
public class User extends AggregateRoot<UUID> {

    private final UUID id;
    private Email email;
    /** Null for Google-only accounts. */
    private @Nullable HashedPassword hashedPassword;

    private FullName fullName;
    private @Nullable String avatarUrl;
    private @Nullable String googleUid;
    private String authProvider;
    private @Nullable String preferences;
    private final Instant createdAt;
    private Instant updatedAt;
    private @Nullable Instant deletedAt;

    private User(
            UUID id,
            Email email,
            @Nullable HashedPassword hashedPassword,
            FullName fullName,
            @Nullable String avatarUrl,
            @Nullable String googleUid,
            String authProvider,
            @Nullable String preferences,
            Instant createdAt,
            Instant updatedAt,
            @Nullable Instant deletedAt) {
        this.id = id;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.googleUid = googleUid;
        this.authProvider = authProvider;
        this.preferences = preferences;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /** Factory method for new email/password user registration. Generates a UUIDv7 primary key. */
    public static User register(Email email, HashedPassword hashedPassword, FullName fullName) {
        Instant now = Instant.now();
        var user = new User(
                UuidCreator.getTimeOrderedEpoch(),
                email,
                hashedPassword,
                fullName,
                null,
                null,
                "EMAIL",
                null,
                now,
                now,
                null);
        user.registerEvent(new UserRegisteredEvent(
                UuidCreator.getTimeOrderedEpoch(), user.id, email.value(), fullName.value(), now));
        return user;
    }

    /**
     * Factory method for Google Sign-In registration.
     * Sets {@code authProvider = "GOOGLE"} and {@code hashedPassword = null}.
     */
    public static User registerWithGoogle(
            Email email, String googleUid, FullName fullName, @Nullable String avatarUrl) {
        Instant now = Instant.now();
        var user = new User(
                UuidCreator.getTimeOrderedEpoch(),
                email,
                null,
                fullName,
                avatarUrl,
                googleUid,
                "GOOGLE",
                null,
                now,
                now,
                null);
        user.registerEvent(new UserRegisteredEvent(
                UuidCreator.getTimeOrderedEpoch(), user.id, email.value(), fullName.value(), now));
        return user;
    }

    /**
     * Links a Google account to this existing email/password account.
     * Sets {@code googleUid} and updates {@code authProvider} to {@code "BOTH"}.
     */
    public void linkGoogle(String googleUid) {
        this.googleUid = googleUid;
        this.authProvider = "BOTH";
        this.updatedAt = Instant.now();
    }

    /** Returns {@code true} if this user has a password set (i.e. not a Google-only account). */
    public boolean hasPassword() {
        return hashedPassword != null;
    }

    /** Reconstitution factory used by the persistence adapter. */
    public static User reconstitute(
            UUID id,
            Email email,
            @Nullable HashedPassword hashedPassword,
            FullName fullName,
            @Nullable String avatarUrl,
            @Nullable String googleUid,
            String authProvider,
            @Nullable String preferences,
            Instant createdAt,
            Instant updatedAt,
            @Nullable Instant deletedAt) {
        return new User(
                id,
                email,
                hashedPassword,
                fullName,
                avatarUrl,
                googleUid,
                authProvider,
                preferences,
                createdAt,
                updatedAt,
                deletedAt);
    }

    /** Soft-deletes this user. Sets {@code deletedAt} to now and updates {@code updatedAt}. */
    public void delete() {
        Instant now = Instant.now();
        this.deletedAt = now;
        this.updatedAt = now;
        registerEvent(new UserDeletedEvent(
                UuidCreator.getTimeOrderedEpoch(), this.id, this.email.value(), now));
    }

    /** Updates the raw JSON preferences string. */
    public void updatePreferences(@Nullable String preferencesJson) {
        this.preferences = preferencesJson;
        this.updatedAt = Instant.now();
    }

    /** Returns {@code true} if this user has been soft-deleted. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public Optional<HashedPassword> getHashedPassword() {
        return Optional.ofNullable(hashedPassword);
    }

    public FullName getFullName() {
        return fullName;
    }

    public Optional<String> getAvatarUrl() {
        return Optional.ofNullable(avatarUrl);
    }

    public Optional<String> getGoogleUid() {
        return Optional.ofNullable(googleUid);
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public Optional<String> getPreferences() {
        return Optional.ofNullable(preferences);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Optional<Instant> getDeletedAt() {
        return Optional.ofNullable(deletedAt);
    }
}
