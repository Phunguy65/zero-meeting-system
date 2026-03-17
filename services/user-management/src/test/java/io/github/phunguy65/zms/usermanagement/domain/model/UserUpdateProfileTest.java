package io.github.phunguy65.zms.usermanagement.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.shared.domain.valueobject.Email;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import io.github.phunguy65.zms.usermanagement.domain.event.UserUpdatedEvent;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.Username;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserUpdateProfileTest {

    private User buildUser(String avatarUrl) {
        return User.reconstitute(
                UserId.of(UuidCreator.getTimeOrderedEpoch()),
                Email.of("alice@example.com"),
                null,
                FullName.of("Alice"),
                null,
                avatarUrl,
                null,
                "EMAIL",
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void updateProfile_fullNameOnly_updatesFullNameAndRegistersEvent() {
        var user = buildUser(null);

        user.updateProfile(FullName.of("New Name"), null, (Username) null);

        assertThat(user.getFullName().value()).isEqualTo("New Name");
        assertThat(user.getAvatarUrl()).isEmpty();
        assertThat(user.getDomainEvents()).hasSize(1);
        assertThat(user.getDomainEvents().get(0)).isInstanceOf(UserUpdatedEvent.class);
        var event = (UserUpdatedEvent) user.getDomainEvents().get(0);
        assertThat(event.fullName()).isEqualTo("New Name");
        assertThat(event.avatarUrl()).isNull();
    }

    @Test
    void updateProfile_avatarUrlOnly_updatesAvatarAndRegistersEvent() {
        var user = buildUser(null);

        user.updateProfile(null, "https://example.com/avatar.png", (Username) null);

        assertThat(user.getFullName().value()).isEqualTo("Alice");
        assertThat(user.getAvatarUrl()).contains("https://example.com/avatar.png");
        assertThat(user.getDomainEvents()).hasSize(1);
        var event = (UserUpdatedEvent) user.getDomainEvents().get(0);
        assertThat(event.avatarUrl()).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    void updateProfile_clearAvatarUrl_setsNullAndRegistersEvent() {
        var user = buildUser("https://example.com/old.png");

        user.updateProfile(null, null, true, null);

        assertThat(user.getAvatarUrl()).isEmpty();
        assertThat(user.getDomainEvents()).hasSize(1);
        var event = (UserUpdatedEvent) user.getDomainEvents().get(0);
        assertThat(event.avatarUrl()).isNull();
    }

    @Test
    void updateProfile_applyAvatarFalse_doesNotChangeAvatar() {
        var user = buildUser("https://example.com/existing.png");

        user.updateProfile(null, null, false, null);

        assertThat(user.getAvatarUrl()).contains("https://example.com/existing.png");
    }

    @Test
    void updateProfile_updatesUpdatedAt() throws InterruptedException {
        var user = buildUser(null);
        Instant before = user.getUpdatedAt();
        Thread.sleep(1);

        user.updateProfile(FullName.of("Updated"), null, (Username) null);

        assertThat(user.getUpdatedAt()).isAfter(before);
    }

    @Test
    void updateProfile_eventCarriesCorrectAuthProvider() {
        var user = buildUser(null);

        user.updateProfile(FullName.of("Alice"), null, (Username) null);

        var event = (UserUpdatedEvent) user.getDomainEvents().get(0);
        assertThat(event.authProvider()).isEqualTo("EMAIL");
        assertThat(event.email()).isEqualTo("alice@example.com");
    }
}
