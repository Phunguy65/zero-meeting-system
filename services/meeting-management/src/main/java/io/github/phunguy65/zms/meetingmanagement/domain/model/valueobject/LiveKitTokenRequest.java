package io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.shared.domain.ValueObject;
import java.util.Objects;

public record LiveKitTokenRequest(
        LiveKitRoomName roomName,
        LiveKitIdentity identity,
        String displayName,
        ParticipantRole role,
        ParticipantAttributes participantAttributes)
        implements ValueObject {

    public LiveKitTokenRequest {
        Objects.requireNonNull(roomName, "roomName must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(participantAttributes, "participantAttributes must not be null");
    }
}
