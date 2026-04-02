package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitEgressId;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitTokenRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipantGrants;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import org.jspecify.annotations.Nullable;

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
     *   <li>{@link ParticipantRole#GUEST} — canSubscribe, canUpdateOwnMetadata (no media
     *       publish)</li>
     * </ul>
     *
     * @param request token generation request, including identity, display name, role, and
     *     token-time participant attributes
     * @return {@link Result.Success} with the signed JWT token, or {@link Result.Failure} with
     * {@link MeetingError.LiveKitUnavailable} if the token cannot be generated
     */
    Result<String, MeetingError> generateToken(LiveKitTokenRequest request);

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
     * Updates a connected participant's runtime profile without changing the history snapshot
     * stored in meeting-management.
     *
     * @param roomName  the LiveKit room containing the participant
     * @param identity  the participant's identity string as embedded in their JWT ({@code sub} claim)
     * @param role      the participant role to keep in sync with LiveKit attributes/permissions
     * @param fullName  the display name to expose in the active room
     * @param avatarUrl the avatar URL to expose in LiveKit attributes; null clears the attribute
     * @return {@link Result.Success} on success, or {@link Result.Failure} with
     * {@link MeetingError.LiveKitUnavailable} if the server is unreachable or rejects the update
     */
    Result<Void, MeetingError> updateParticipantProfile(
            LiveKitRoomName roomName,
            String identity,
            ParticipantRole role,
            String fullName,
            @Nullable String avatarUrl);

    /**
     * Starts a room-composite egress for a meeting-scoped recording file in S3-compatible storage.
     */
    Result<LiveKitEgressId, MeetingError> startRoomCompositeEgress(
            MeetingId meetingId, LiveKitRoomName roomName);

    /**
     * Stops a running or pending LiveKit egress session.
     */
    Result<Void, MeetingError> stopEgress(LiveKitEgressId egressId);

    /**
     * Deletes a LiveKit room and disconnects all participants.
     *
     * @return {@link Result.Success} on success, or {@link Result.Failure} with
     * {@link MeetingError.LiveKitUnavailable} if the server is unreachable
     */
    Result<Void, MeetingError> deleteRoom(LiveKitRoomName roomName);
}
