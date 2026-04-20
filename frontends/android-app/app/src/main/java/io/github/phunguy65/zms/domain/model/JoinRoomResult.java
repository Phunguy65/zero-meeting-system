package io.github.phunguy65.zms.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Represents the result of a join room request.
 * Can be APPROVED (with token), PENDING (waiting for host approval), or DENIED.
 */
public class JoinRoomResult {

    /**
     * The status of the join request.
     */
    public enum Status {
        /** Request approved, token is available for room connection. */
        APPROVED,
        /** Request pending host approval, need to subscribe to SSE for updates. */
        PENDING,
        /** Request denied by host or system. */
        DENIED
    }

    private final Status status;
    private final String livekitToken;
    private final String roomName;
    private final String requestId;
    private final String denyReason;
    private final String meetingUuid;

    private JoinRoomResult(
            Status status,
            @Nullable String livekitToken,
            @Nullable String roomName,
            @Nullable String requestId,
            @Nullable String denyReason,
            @Nullable String meetingUuid) {
        this.status = status;
        this.livekitToken = livekitToken;
        this.roomName = roomName;
        this.requestId = requestId;
        this.denyReason = denyReason;
        this.meetingUuid = meetingUuid;
    }

    /**
     * Creates an APPROVED result with the LiveKit access token, room name, and meeting UUID.
     */
    public static JoinRoomResult approved(String livekitToken, String roomName, String meetingUuid) {
        return new JoinRoomResult(Status.APPROVED, livekitToken, roomName, null, null, meetingUuid);
    }

    /**
     * Creates an APPROVED result with the LiveKit access token and room name.
     * @deprecated Use {@link #approved(String, String, String)} with meeting UUID instead.
     */
    @Deprecated
    public static JoinRoomResult approved(String livekitToken, String roomName) {
        return new JoinRoomResult(Status.APPROVED, livekitToken, roomName, null, null, null);
    }

    /**
     * Creates an APPROVED result with only the LiveKit access token.
     * @deprecated Use {@link #approved(String, String, String)} with room name and meeting UUID instead.
     */
    @Deprecated
    public static JoinRoomResult approved(String livekitToken) {
        return new JoinRoomResult(Status.APPROVED, livekitToken, null, null, null, null);
    }

    /**
     * Creates a PENDING result with the join request ID and meeting UUID for SSE subscription.
     */
    public static JoinRoomResult pending(String requestId, String meetingUuid) {
        return new JoinRoomResult(Status.PENDING, null, null, requestId, null, meetingUuid);
    }

    /**
     * Creates a PENDING result with the join request ID for SSE subscription.
     * @deprecated Use {@link #pending(String, String)} with meeting UUID instead.
     */
    @Deprecated
    public static JoinRoomResult pending(String requestId) {
        return new JoinRoomResult(Status.PENDING, null, null, requestId, null, null);
    }

    /**
     * Creates a DENIED result with the denial reason.
     */
    public static JoinRoomResult denied(String reason) {
        return new JoinRoomResult(Status.DENIED, null, null, null, reason, null);
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Returns the LiveKit access token. Only valid when status is APPROVED.
     */
    public String getLivekitToken() {
        return livekitToken;
    }

    /**
     * Returns the room name from the backend. Only valid when status is APPROVED.
     */
    public String getRoomName() {
        return roomName;
    }

    /**
     * Returns the join request ID. Only valid when status is PENDING.
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Returns the denial reason. Only valid when status is DENIED.
     */
    public String getDenyReason() {
        return denyReason;
    }

    /**
     * Returns the resolved meeting UUID. Available for APPROVED and PENDING status.
     * Used by ViewModel to set the correct meeting context for API calls.
     */
    @Nullable public String getMeetingUuid() {
        return meetingUuid;
    }

    public boolean isApproved() {
        return status == Status.APPROVED;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public boolean isDenied() {
        return status == Status.DENIED;
    }
}
