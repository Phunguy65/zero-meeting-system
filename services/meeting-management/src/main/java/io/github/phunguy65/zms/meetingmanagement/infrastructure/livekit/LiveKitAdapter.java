package io.github.phunguy65.zms.meetingmanagement.infrastructure.livekit;

import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipantGrants;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.config.LiveKitProperties;
import io.github.phunguy65.zms.shared.domain.Result;
import io.livekit.server.*;
import java.io.IOException;
import java.util.List;
import livekit.LivekitModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import retrofit2.Response;

/**
 * LiveKit adapter — generates JWT access tokens and manages rooms via the LiveKit Server SDK.
 *
 * <p>Token identity format:
 * <ul>
 *   <li>Authenticated user: {@code "<userId>:<deviceId>"} — supports multi-device without
 *       triggering {@code DUPLICATE_IDENTITY} disconnects.
 *   <li>Guest: {@code "guest:<deviceId>"}
 * </ul>
 *
 * <p>Permission matrix by role:
 * <ul>
 *   <li>HOST — roomAdmin, canPublish, canPublishData, canSubscribe, canUpdateOwnMetadata
 *   <li>PARTICIPANT — canPublish, canPublishData, canSubscribe, canUpdateOwnMetadata
 *   <li>GUEST — canPublishData, canSubscribe (subscribe + chat only)
 * </ul>
 *
 * <p>Room operations use blocking {@code call.execute()}, which is safe on virtual threads
 * (enabled via {@code spring.threads.virtual.enabled=true}). All infrastructure exceptions are
 * caught internally and returned as {@link MeetingError.LiveKitUnavailable}.
 */
@Component
public class LiveKitAdapter implements LiveKitPort {

    private static final Logger log = LoggerFactory.getLogger(LiveKitAdapter.class);

    private final LiveKitProperties props;
    private final RoomServiceClient roomServiceClient;

    public LiveKitAdapter(LiveKitProperties props, RoomServiceClient roomServiceClient) {
        this.props = props;
        this.roomServiceClient = roomServiceClient;
    }

    @Override
    public Result<String, MeetingError> generateToken(
            LiveKitRoomName roomName,
            LiveKitIdentity identity,
            String displayName,
            ParticipantRole role) {
        try {
            AccessToken token = new AccessToken(props.getApiKey(), props.getApiSecret());
            token.setIdentity(identity.value());
            token.setName(displayName);
            token.setTtl(props.getTokenExpirySeconds() * 1_000L);
            token.addGrants(new RoomJoin(true), new RoomName(roomName.value()));
            token.addGrants(buildRoleGrants(role).toArray(new VideoGrant[0]));

            return Result.success(token.toJwt());
        } catch (Exception e) {
            log.warn(
                    "Failed to generate LiveKit token for room '{}': {}",
                    roomName.value(),
                    e.getMessage());
            return Result.failure(new MeetingError.LiveKitUnavailable(e.getMessage()));
        }
    }

    private List<VideoGrant> buildRoleGrants(ParticipantRole role) {
        return switch (role) {
            case HOST ->
                List.of(
                        new RoomAdmin(true),
                        new CanPublish(true),
                        new CanPublishData(true),
                        new CanSubscribe(true),
                        new CanUpdateOwnMetadata(true));
            case PARTICIPANT ->
                List.of(
                        new CanPublish(true),
                        new CanPublishData(true),
                        new CanSubscribe(true),
                        new CanUpdateOwnMetadata(true));
            case GUEST ->
                List.of(
                        new CanPublish(false),
                        new CanPublishData(false),
                        new CanSubscribe(true),
                        new CanUpdateOwnMetadata(false));
        };
    }

    // -------------------------------------------------------------------------
    // Room management
    // -------------------------------------------------------------------------

    @Override
    public Result<Void, MeetingError> updateParticipantPermissions(
            LiveKitRoomName roomName, String identity, ParticipantGrants grants) {
        LivekitModels.ParticipantPermission permission =
                LivekitModels.ParticipantPermission.newBuilder()
                        .setCanPublish(grants.canPublish())
                        .setCanPublishData(grants.canPublishData())
                        .setCanSubscribe(grants.canSubscribe())
                        .build();

        try {
            Response<LivekitModels.ParticipantInfo> response = roomServiceClient
                    .updateParticipant(roomName.value(), identity, null, null, permission, null)
                    .execute();

            if (!response.isSuccessful()) {
                String msg = "HTTP %d: %s".formatted(response.code(), response.message());
                log.warn(
                        "Failed to update permissions for participant '{}' in room '{}': {}",
                        identity,
                        roomName.value(),
                        msg);
                return Result.failure(new MeetingError.LiveKitUnavailable(msg));
            }

            log.info(
                    "Updated permissions for participant '{}' in room '{}': canPublish={}, canPublishData={}, canSubscribe={}",
                    identity,
                    roomName.value(),
                    grants.canPublish(),
                    grants.canPublishData(),
                    grants.canSubscribe());

            return Result.success();

        } catch (IOException e) {
            log.warn(
                    "Network error updating participant '{}' in room '{}': {}",
                    identity,
                    roomName.value(),
                    e.getMessage());
            return Result.failure(new MeetingError.LiveKitUnavailable(e.getMessage()));
        }
    }

    @Override
    public Result<Void, MeetingError> deleteRoom(LiveKitRoomName roomName) {
        try {
            Response<?> response =
                    roomServiceClient.deleteRoom(roomName.value()).execute();

            if (!response.isSuccessful()) {
                String msg = "HTTP %d: %s".formatted(response.code(), response.message());
                log.warn("Failed to delete LiveKit room '{}': {}", roomName.value(), msg);
                return Result.failure(new MeetingError.LiveKitUnavailable(msg));
            }

            log.info("Deleted LiveKit room: {}", roomName.value());
            return Result.success();

        } catch (IOException e) {
            log.warn("Network error deleting room '{}': {}", roomName.value(), e.getMessage());
            return Result.failure(new MeetingError.LiveKitUnavailable(e.getMessage()));
        }
    }
}
