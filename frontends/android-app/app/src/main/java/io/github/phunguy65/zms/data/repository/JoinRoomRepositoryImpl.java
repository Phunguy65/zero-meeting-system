package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.remote.api.JoinRequestsApi;
import io.github.phunguy65.zms.data.remote.api.MeetingsApi;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementJoinRequestRequest;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementRequestJoinResponse;
import io.github.phunguy65.zms.data.remote.interceptor.AndroidErrorTranslator;
import io.github.phunguy65.zms.data.remote.interceptor.ApiErrorException;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.data.remote.sse.JoinRequestSseClient;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.model.JoinRoomResult;
import io.github.phunguy65.zms.domain.repository.JoinRoomRepository;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;
import retrofit2.Response;

/**
 * Implementation of {@link JoinRoomRepository} backed by the join requests API.
 * Handles join request submission and SSE approval subscription.
 * Supports both direct UUID and short code lookups for meeting identification.
 */
public class JoinRoomRepositoryImpl implements JoinRoomRepository {

    private final JoinRequestsApi joinRequestsApi;
    private final MeetingsApi meetingsApi;
    private final JoinRequestSseClient sseClient;
    private final SessionRepository sessionRepository;
    private final AndroidErrorTranslator errorTranslator;
    private final Executor ioExecutor;

    @Inject
    public JoinRoomRepositoryImpl(
            JoinRequestsApi joinRequestsApi,
            MeetingsApi meetingsApi,
            JoinRequestSseClient sseClient,
            SessionRepository sessionRepository,
            AndroidErrorTranslator errorTranslator,
            @IoExecutor Executor ioExecutor) {
        this.joinRequestsApi = joinRequestsApi;
        this.meetingsApi = meetingsApi;
        this.sseClient = sseClient;
        this.sessionRepository = sessionRepository;
        this.errorTranslator = errorTranslator;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<JoinRoomResult> requestJoin(
            String meetingCode,
            @Nullable String meetingUuid,
            String displayName,
            String deviceId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID meetingId = resolveMeetingId(meetingCode, meetingUuid);

                        MeetingManagementJoinRequestRequest request =
                                new MeetingManagementJoinRequestRequest()
                                        .displayName(displayName)
                                        .deviceId(deviceId);

                        Response<MeetingManagementRequestJoinResponse> response =
                                joinRequestsApi.requestJoin(meetingId, request).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException("Join request failed: HTTP " + response.code());
                        }

                        return mapToJoinRoomResult(response.body(), meetingId.toString());
                    } catch (Exception e) {
                        throw new CompletionException(translateException(e));
                    }
                },
                ioExecutor);
    }

    /**
     * Resolves the meeting ID from the provided UUID or by looking up the short code.
     * Priority: meetingUuid > meetingCode as UUID > meetingCode lookup via API
     */
    private UUID resolveMeetingId(String meetingCode, @Nullable String meetingUuid)
            throws IOException {
        if (meetingUuid != null && !meetingUuid.isEmpty()) {
            try {
                return UUID.fromString(meetingUuid);
            } catch (IllegalArgumentException ignored) {
            }
        }

        try {
            return UUID.fromString(meetingCode);
        } catch (IllegalArgumentException ignored) {
        }

        Response<MeetingManagementMeetingResponse> lookupResponse =
                meetingsApi.getMeetingByShortCode(meetingCode).execute();

        if (!lookupResponse.isSuccessful() || lookupResponse.body() == null) {
            int code = lookupResponse.code();
            if (code == 404) {
                throw new JoinRoomException("Meeting not found");
            }
            throw new IOException("Failed to resolve meeting code: HTTP " + code);
        }

        UUID resolvedId = lookupResponse.body().getId();
        if (resolvedId == null) {
            throw new JoinRoomException("Meeting response missing ID");
        }
        return resolvedId;
    }

    @Override
    public void subscribeToApproval(String requestId, ApprovalEventListener listener) {
        String authToken = sessionRepository.getAccessToken();

        sseClient.subscribe(requestId, authToken, listener);
    }

    @Override
    public void cancelApprovalSubscription() {
        sseClient.cancel();
    }

    /**
     * Maps the API response to a domain JoinRoomResult.
     * Includes the resolved meeting UUID for downstream API calls.
     */
    private JoinRoomResult mapToJoinRoomResult(
            MeetingManagementRequestJoinResponse response, String resolvedMeetingUuid) {
        if (response.getStatus() == null) {
            return JoinRoomResult.denied("Unknown response status");
        }

        switch (response.getStatus()) {
            case APPROVED:
                String token = response.getToken();
                if (token == null || token.isEmpty()) {
                    return JoinRoomResult.denied("No access token provided");
                }
                String roomName = response.getRoomName();
                return JoinRoomResult.approved(token, roomName, resolvedMeetingUuid);

            case PENDING:
                UUID requestId = response.getRequestId();
                if (requestId == null) {
                    return JoinRoomResult.denied("No request ID for pending approval");
                }
                return JoinRoomResult.pending(requestId.toString(), resolvedMeetingUuid);

            case DENIED:
                return JoinRoomResult.denied("Join request denied");

            case EXPIRED:
                return JoinRoomResult.denied("Join request expired");

            default:
                return JoinRoomResult.denied("Unknown status: " + response.getStatus());
        }
    }

    /**
     * Translates various exceptions into user-friendly localized messages.
     * Preserves JoinRoomException messages as they are already user-friendly.
     */
    private Exception translateException(Exception e) {
        if (e instanceof JoinRoomException) {
            return e;
        }

        if (e instanceof ApiFailException) {
            ApiFailException failException = (ApiFailException) e;
            String translatedMessage =
                    errorTranslator.translate(failException.getCode(), failException.getMessage());
            return new JoinRoomException(translatedMessage);
        }

        if (e instanceof ApiErrorException) {
            return new JoinRoomException(
                    errorTranslator.translate("SERVER_ERROR", e.getMessage()));
        }

        if (e instanceof UnknownHostException || e instanceof SocketTimeoutException) {
            return new JoinRoomException(errorTranslator.translate(
                    "NETWORK_ERROR", "No internet connection. Please check your network."));
        }

        if (e instanceof IOException) {
            return new JoinRoomException(errorTranslator.translate(
                    "SERVER_ERROR", "Something went wrong. Please try again later."));
        }

        if (e instanceof CompletionException && e.getCause() != null) {
            return translateException((Exception) e.getCause());
        }

        return new JoinRoomException(
                errorTranslator.translate("UNKNOWN_ERROR", "An unexpected error occurred."));
    }

    /**
     * Custom exception for join room errors with user-friendly messages.
     */
    public static class JoinRoomException extends RuntimeException {
        public JoinRoomException(String message) {
            super(message);
        }
    }
}
