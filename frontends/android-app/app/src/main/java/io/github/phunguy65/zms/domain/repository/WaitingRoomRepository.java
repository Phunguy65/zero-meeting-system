package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.JoinRequestItem;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository contract for host waiting-room operations including
 * pending request management and meeting event subscription.
 */
public interface WaitingRoomRepository {

    CompletableFuture<List<JoinRequestItem>> listPendingRequests(String meetingId);

    CompletableFuture<Void> approveRequest(String meetingId, String requestId);

    CompletableFuture<Void> denyRequest(String meetingId, String requestId);

    CompletableFuture<Void> approveAll(String meetingId);

    void subscribeToHostEvents(String meetingId, String authToken, HostEventListener listener);

    void cancelHostSubscription();

    /**
     * Listener for host-side meeting events received via SSE.
     */
    interface HostEventListener {

        void onConnected();

        void onJoinRequestCreated(String requestId, String meetingId, String displayName);

        void onJoinRequestExpired(String requestId);

        void onParticipantKicked(String meetingId, String kickedUserId, String displayName);

        void onError(String message);
    }
}
