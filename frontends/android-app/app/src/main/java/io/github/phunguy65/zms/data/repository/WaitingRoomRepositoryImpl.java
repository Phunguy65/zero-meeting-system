package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.remote.api.JoinRequestsApi;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementJoinRequestResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementOffsetScrollResponseJoinRequestResponse;
import io.github.phunguy65.zms.data.remote.sse.MeetingEventSseClient;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.model.JoinRequestItem;
import io.github.phunguy65.zms.domain.repository.WaitingRoomRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import retrofit2.Response;

/**
 * Implementation of {@link WaitingRoomRepository} backed by the join requests API
 * and meeting event SSE client for host waiting-room operations.
 */
public class WaitingRoomRepositoryImpl implements WaitingRoomRepository {

    private static final int DEFAULT_PAGE_SIZE = 100;

    private final JoinRequestsApi joinRequestsApi;
    private final MeetingEventSseClient sseClient;
    private final Executor ioExecutor;

    @Inject
    public WaitingRoomRepositoryImpl(
            JoinRequestsApi joinRequestsApi,
            MeetingEventSseClient sseClient,
            @IoExecutor Executor ioExecutor) {
        this.joinRequestsApi = joinRequestsApi;
        this.sseClient = sseClient;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<List<JoinRequestItem>> listPendingRequests(String meetingId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID uuid = UUID.fromString(meetingId);
                Response<MeetingManagementOffsetScrollResponseJoinRequestResponse> response =
                        joinRequestsApi.listJoinRequests(uuid, DEFAULT_PAGE_SIZE, 0).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to list join requests: HTTP " + response.code());
                }

                return mapToJoinRequestItems(response.body());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> approveRequest(String meetingId, String requestId) {
        return CompletableFuture.runAsync(() -> {
            try {
                UUID meetingUuid = UUID.fromString(meetingId);
                UUID requestUuid = UUID.fromString(requestId);
                Response<Void> response =
                        joinRequestsApi.approveJoinRequest(meetingUuid, requestUuid).execute();

                if (!response.isSuccessful()) {
                    throw new IOException("Failed to approve request: HTTP " + response.code());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> denyRequest(String meetingId, String requestId) {
        return CompletableFuture.runAsync(() -> {
            try {
                UUID meetingUuid = UUID.fromString(meetingId);
                UUID requestUuid = UUID.fromString(requestId);
                Response<Void> response =
                        joinRequestsApi.denyJoinRequest(meetingUuid, requestUuid).execute();

                if (!response.isSuccessful()) {
                    throw new IOException("Failed to deny request: HTTP " + response.code());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }

    @Override
    public CompletableFuture<Void> approveAll(String meetingId) {
        return CompletableFuture.runAsync(() -> {
            try {
                UUID meetingUuid = UUID.fromString(meetingId);
                Response<?> response =
                        joinRequestsApi.approveAllJoinRequests(meetingUuid).execute();

                if (!response.isSuccessful()) {
                    throw new IOException("Failed to approve all: HTTP " + response.code());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }

    @Override
    public void subscribeToHostEvents(
            String meetingId, String authToken, HostEventListener listener) {
        sseClient.subscribe(meetingId, authToken, new MeetingEventSseClient.MeetingEventListener() {
            @Override
            public void onConnected() {
            }

            @Override
            public void onDisconnected() {
                listener.onError("Connection closed");
            }

            @Override
            public void onJoinRequestCreated(
                    String requestId, String eventMeetingId, String displayName) {
                listener.onJoinRequestCreated(requestId, eventMeetingId, displayName);
            }

            @Override
            public void onJoinRequestExpired(String requestId) {
                listener.onJoinRequestExpired(requestId);
            }

            @Override
            public void onParticipantKicked(
                    String eventMeetingId, String kickedUserId, String displayName) {
                listener.onParticipantKicked(eventMeetingId, kickedUserId, displayName);
            }

            @Override
            public void onError(String message) {
                listener.onError(message);
            }
        });
    }

    @Override
    public void cancelHostSubscription() {
        sseClient.cancel();
    }

    private List<JoinRequestItem> mapToJoinRequestItems(
            MeetingManagementOffsetScrollResponseJoinRequestResponse response) {
        List<JoinRequestItem> items = new ArrayList<>();
        List<MeetingManagementJoinRequestResponse> content = response.getContent();
        if (content == null) return items;

        for (MeetingManagementJoinRequestResponse dto : content) {
            if (dto.getStatus() != MeetingManagementJoinRequestResponse.StatusEnum.PENDING) {
                continue;
            }
            items.add(new JoinRequestItem(
                    dto.getId() != null ? dto.getId().toString() : "",
                    dto.getMeetingId() != null ? dto.getMeetingId().toString() : "",
                    dto.getDisplayName() != null ? dto.getDisplayName() : "",
                    dto.getRequestedAt() != null ? dto.getRequestedAt().toString() : ""));
        }
        return items;
    }
}
