package io.github.phunguy65.zms.usermanagement.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.shared.domain.AggregateRoot;
import io.github.phunguy65.zms.usermanagement.domain.event.UserDeletedEvent;
import io.github.phunguy65.zms.usermanagement.domain.event.UserRegisteredEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * User aggregate root. Represents a registered account in the system.
 */
public class User extends AggregateRoot<UUID> {

    private final UUID id;
    private Email email;
    private HashedPassword hashedPassword;
    private FullName fullName;
    private String avatarUrl;
    private String preferences;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private User(
            UUID id,
            Email email,
            HashedPassword hashedPassword,
            FullName fullName,
            String avatarUrl,
            String preferences,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        this.id = id;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.preferences = preferences;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    /** Factory method for new user registration. Generates a UUIDv7 primary key. */
    public static User register(Email email, HashedPassword hashedPassword, FullName fullName) {
        Instant now = Instant.now();
        var user = new User(
                UuidCreator.getTimeOrderedEpoch(),
                email,
                hashedPassword,
                fullName,
                null,
                null,
                now,
                now,
                null);
        user.registerEvent(new UserRegisteredEvent(
                UuidCreator.getTimeOrderedEpoch(), user.id, email.value(), fullName.value(), now));
        return user;
    }

    /** Reconstitution factory used by the persistence adapter. */
    public static User reconstitute(
            UUID id,
            Email email,
            HashedPassword hashedPassword,
            FullName fullName,
            String avatarUrl,
            String preferences,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt) {
        return new User(
                id,
                email,
                hashedPassword,
                fullName,
                avatarUrl,
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

    public HashedPassword getHashedPassword() {
        return hashedPassword;
    }

    public FullName getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPreferences() {
        return preferences;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
