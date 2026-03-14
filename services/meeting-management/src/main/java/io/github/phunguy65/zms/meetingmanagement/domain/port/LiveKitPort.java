package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Port for interacting with the LiveKit media server. */
public interface LiveKitPort {

    /**
     * Generates a signed JWT token for a participant to join a LiveKit room.
     *
     * @param roomName    the LiveKit room name
     * @param userId      the participant's user ID (null for guests)
     * @param displayName the participant's display name
     * @param role        HOST or PARTICIPANT (affects publish permissions)
     * @return signed JWT token string
     */
    String generateToken(
            LiveKitRoomName roomName,
            @Nullable UUID userId,
            String displayName,
            ParticipantRole role);

    /** Creates a LiveKit room. No-op if the room already exists. */
    void createRoom(LiveKitRoomName roomName);

    /** Deletes a LiveKit room and disconnects all participants. */
    void deleteRoom(LiveKitRoomName roomName);
}
