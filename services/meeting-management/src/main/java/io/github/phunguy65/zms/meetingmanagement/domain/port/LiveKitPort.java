package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipantGrants;
import io.github.phunguy65.zms.shared.domain.Result;

/**
 * Port for interacting with the LiveKit media server.
 */
public interface LiveKitPort {

    /**
     * Generates a signed JWT token for a participant to join a LiveKit room.
     *
     * <p>Permission matrix by role:
     * <ul>
     *   <li>{@link ParticipantRole#HOST} — roomAdmin, canPublish, canPublishData, canSubscribe,
     *       canUpdateOwnMetadata</li>
     *   <li>{@link ParticipantRole#PARTICIPANT} — canPublish, canPublishData, canSubscribe,
     *       canUpdateOwnMetadata</li>
     *   <li>{@link ParticipantRole#GUEST} — canPublishData, canSubscribe (no media publish,
     *       no metadata update)</li>
     * </ul>
     *
     * @param roomName    the LiveKit room name
     * @param identity    the participant's LiveKit identity ({@code sub} JWT claim);
     *                    format: {@code "userId:deviceId"} or {@code "guest:deviceId"}
     * @param displayName the participant's display name shown to others
     * @param role        HOST, PARTICIPANT, or GUEST — determines LiveKit grants
     * @return {@link Result.Success} with the signed JWT token, or {@link Result.Failure} with
     * {@link MeetingError.LiveKitUnavailable} if the token cannot be generated
     */
    Result<String, MeetingError> generateToken(
            LiveKitRoomName roomName,
            LiveKitIdentity identity,
            String displayName,
            ParticipantRole role);

    /**
     * Updates a connected participant's permissions mid-session without disconnecting them.
     *
     * @param roomName the LiveKit room containing the participant
     * @param identity the participant's identity string as embedded in their JWT ({@code sub} claim)
     * @param grants   the new permission set to apply
     * @return {@link Result.Success} on success, or {@link Result.Failure} with
     * {@link MeetingError.LiveKitUnavailable} if the server is unreachable
     */
    Result<Void, MeetingError> updateParticipantPermissions(
            LiveKitRoomName roomName, String identity, ParticipantGrants grants);

    /**
     * Deletes a LiveKit room and disconnects all participants.
     *
     * @return {@link Result.Success} on success, or {@link Result.Failure} with
     * {@link MeetingError.LiveKitUnavailable} if the server is unreachable
     */
    Result<Void, MeetingError> deleteRoom(LiveKitRoomName roomName);
}
