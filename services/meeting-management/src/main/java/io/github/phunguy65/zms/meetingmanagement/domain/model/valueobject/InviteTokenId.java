package io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject;

import io.github.phunguy65.zms.shared.domain.ValueObject;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identity for an {@link io.github.phunguy65.zms.meetingmanagement.domain.model.InviteToken}.
 */
public record InviteTokenId(UUID value) implements ValueObject {

    public InviteTokenId {
        Objects.requireNonNull(value, "InviteTokenId value must not be null");
    }

    public static InviteTokenId of(UUID value) {
        return new InviteTokenId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
