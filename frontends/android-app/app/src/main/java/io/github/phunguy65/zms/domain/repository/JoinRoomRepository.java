package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.JoinRoomResult;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;

/**
 * Repository interface for backend room join operations.
 * Handles join request submission and pending approval subscription lifecycle.
 */
public interface JoinRoomRepository {

    /**
     * Requests to join a meeting room.
     *
     * @param meetingCode the meeting short code (for lookup if meetingUuid is null)
     * @param meetingUuid the meeting UUID (if available, used directly for API call)
     * @param displayName the display name for the participant
     * @param deviceId    the device identifier
     * @return a CompletableFuture that completes with the join result
     */
    CompletableFuture<JoinRoomResult> requestJoin(
            String meetingCode,
            @Nullable String meetingUuid,
            String displayName,
            String deviceId);

    /**
     * Subscribes to approval status updates for a pending join request.
     * Uses SSE to receive real-time updates.
     *
     * @param requestId the join request ID from a PENDING response
     * @param listener  the listener to receive approval events
     */
    void subscribeToApproval(String requestId, ApprovalEventListener listener);

    /**
     * Cancels an active approval subscription.
     * Should be called when leaving pre-join or when approval is received.
     */
    void cancelApprovalSubscription();

    /**
     * Listener interface for join request approval events.
     */
    interface ApprovalEventListener {

        /** Called when the join request is approved. */
        void onApproved(String livekitToken);

        /** Called when the join request is denied. */
        void onDenied(String reason);

        /** Called when the join request expires. */
        void onExpired();

        /** Called when the SSE connection encounters an error. */
        void onError(String message);
    }
}
