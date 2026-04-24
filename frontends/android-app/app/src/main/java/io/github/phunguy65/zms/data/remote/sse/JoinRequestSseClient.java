package io.github.phunguy65.zms.data.remote.sse;

import android.os.Handler;
import android.os.Looper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.zms.domain.repository.JoinRoomRepository.ApprovalEventListener;
import io.github.phunguy65.zms.frontends.BuildConfig;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

/**
 * SSE client for subscribing to join request approval events.
 *
 * <p>Uses OkHttp EventSource with Handler-driven lifecycle for UI updates.
 * Automatically retries failed connections up to {@value #MAX_RETRIES} times using
 * exponential back-off (1s, 2s, 4s). Retries are suppressed after terminal events
 * (approved/denied/expired) or explicit cancellation.
 */
public class JoinRequestSseClient {

    private static final String SSE_PATH = "/api/v1/joinRequests/%s/events";
    private static final int SSE_TIMEOUT_MINUTES = 10;
    private static final int MAX_RETRIES = 3;
    private static final int BASE_RETRY_DELAY_MS = 1000;

    private final OkHttpClient httpClient;
    private final Handler mainHandler;
    private final ObjectMapper objectMapper;

    private EventSource currentEventSource;
    private ApprovalEventListener currentListener;

    private volatile boolean terminated;
    private int retryCount;
    private String savedRequestId;
    private String savedAuthToken;
    private ApprovalEventListener savedListener;

    @Inject
    public JoinRequestSseClient(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient
                .newBuilder()
                .readTimeout(SSE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.objectMapper = objectMapper;
    }

    /**
     * Subscribes to approval events for the given join request.
     *
     * @param requestId the join request ID
     * @param authToken the authentication token (may be null for guest)
     * @param listener  the listener to receive events
     */
    public void subscribe(String requestId, String authToken, ApprovalEventListener listener) {
        cancel();

        this.terminated = false;
        this.retryCount = 0;
        this.savedRequestId = requestId;
        this.savedAuthToken = authToken;
        this.savedListener = listener;
        this.currentListener = listener;

        openEventSource(requestId, authToken);
    }

    /**
     * Cancels the current SSE subscription.
     */
    public void cancel() {
        terminated = true;
        if (currentEventSource != null) {
            currentEventSource.cancel();
            currentEventSource = null;
        }
        currentListener = null;
    }

    private void openEventSource(String requestId, String authToken) {
        String url = BuildConfig.API_BASE_URL + String.format(SSE_PATH, requestId);
        Request.Builder requestBuilder =
                new Request.Builder().url(url).header("Accept", "text/event-stream");

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        EventSource.Factory factory = EventSources.createFactory(httpClient);
        currentEventSource = factory.newEventSource(requestBuilder.build(), new SseEventListener());
    }

    private void scheduleRetry() {
        if (terminated || retryCount >= MAX_RETRIES) {
            if (!terminated && currentListener != null) {
                mainHandler.post(() -> {
                    if (currentListener != null) {
                        currentListener.onError(
                                "Connection failed after " + MAX_RETRIES + " retries");
                    }
                });
            }
            return;
        }

        int delayMs = BASE_RETRY_DELAY_MS * (1 << retryCount);
        retryCount++;

        mainHandler.postDelayed(
                () -> {
                    if (!terminated && savedListener != null) {
                        currentListener = savedListener;
                        openEventSource(savedRequestId, savedAuthToken);
                    }
                },
                delayMs);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ApprovedEventData {
        public String token;
        public String roomName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DeniedEventData {
        public String reason;
    }

    /**
     * Internal listener that parses SSE events and dispatches to the approval listener.
     */
    private class SseEventListener extends EventSourceListener {

        @Override
        public void onOpen(EventSource eventSource, Response response) {
            // Connection opened, waiting for events
        }

        @Override
        public void onEvent(EventSource eventSource, String id, String type, String data) {
            if (currentListener == null) return;

            switch (type) {
                case "join_request_approved":
                    terminated = true;
                    mainHandler.post(() -> {
                        if (currentListener == null) return;
                        String token = parseApprovedToken(data);
                        currentListener.onApproved(token);
                    });
                    break;

                case "join_request_denied":
                    terminated = true;
                    mainHandler.post(() -> {
                        if (currentListener == null) return;
                        String reason = parseDeniedReason(data);
                        currentListener.onDenied(reason);
                    });
                    break;

                case "join_request_expired":
                    terminated = true;
                    mainHandler.post(() -> {
                        if (currentListener == null) return;
                        currentListener.onExpired();
                    });
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onClosed(EventSource eventSource) {
            // Connection closed normally
        }

        @Override
        public void onFailure(EventSource eventSource, Throwable t, Response response) {
            if (terminated || currentListener == null) return;

            scheduleRetry();
        }

        private String parseApprovedToken(String data) {
            if (data == null || data.isEmpty()) return "";
            try {
                ApprovedEventData parsed = objectMapper.readValue(data, ApprovedEventData.class);
                return parsed != null && parsed.token != null ? parsed.token : "";
            } catch (Exception e) {
                return "";
            }
        }

        private String parseDeniedReason(String data) {
            if (data == null || data.isEmpty()) return "Request denied";
            try {
                DeniedEventData parsed = objectMapper.readValue(data, DeniedEventData.class);
                return parsed != null && parsed.reason != null ? parsed.reason : "Request denied";
            } catch (Exception e) {
                return "Request denied";
            }
        }
    }
}
