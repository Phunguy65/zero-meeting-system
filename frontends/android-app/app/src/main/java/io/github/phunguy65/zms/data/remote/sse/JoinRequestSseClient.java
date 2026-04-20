package io.github.phunguy65.zms.data.remote.sse;

import android.os.Handler;
import android.os.Looper;
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
 * Uses OkHttp EventSource with Handler-driven lifecycle for UI updates.
 */
public class JoinRequestSseClient {

    private static final String SSE_PATH = "/api/v1/joinRequests/%s/events";
    private static final int SSE_TIMEOUT_MINUTES = 5;

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    private EventSource currentEventSource;
    private ApprovalEventListener currentListener;

    @Inject
    public JoinRequestSseClient(OkHttpClient httpClient) {
        // Create SSE-specific client with longer read timeout
        this.httpClient = httpClient.newBuilder()
                .readTimeout(SSE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Subscribes to approval events for the given join request.
     *
     * @param requestId the join request ID
     * @param authToken the authentication token (may be null for guest)
     * @param listener  the listener to receive events
     */
    public void subscribe(String requestId, String authToken, ApprovalEventListener listener) {
        // Cancel any existing subscription
        cancel();

        this.currentListener = listener;

        String url = BuildConfig.API_BASE_URL + String.format(SSE_PATH, requestId);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream");

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        EventSource.Factory factory = EventSources.createFactory(httpClient);
        currentEventSource = factory.newEventSource(requestBuilder.build(), new SseEventListener());
    }

    /**
     * Cancels the current SSE subscription.
     */
    public void cancel() {
        if (currentEventSource != null) {
            currentEventSource.cancel();
            currentEventSource = null;
        }
        currentListener = null;
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

            mainHandler.post(() -> {
                if (currentListener == null) return;

                switch (type) {
                    case "join_request_approved":
                        // Data contains the LiveKit token
                        String token = extractToken(data);
                        currentListener.onApproved(token);
                        cancel();
                        break;

                    case "join_request_denied":
                        String reason = extractReason(data);
                        currentListener.onDenied(reason);
                        cancel();
                        break;

                    case "join_request_expired":
                        currentListener.onExpired();
                        cancel();
                        break;

                    default:
                        // Unknown event type, ignore
                        break;
                }
            });
        }

        @Override
        public void onClosed(EventSource eventSource) {
            // Connection closed normally
        }

        @Override
        public void onFailure(EventSource eventSource, Throwable t, Response response) {
            if (currentListener == null) return;

            mainHandler.post(() -> {
                if (currentListener != null) {
                    String message = t != null ? t.getMessage() : "Connection failed";
                    currentListener.onError(message);
                }
            });
        }

        /**
         * Extracts the LiveKit token from the SSE data payload.
         * Expected format: {"token": "..."}
         */
        private String extractToken(String data) {
            if (data == null || data.isEmpty()) return "";
            // Simple JSON extraction for token field
            int tokenStart = data.indexOf("\"token\"");
            if (tokenStart < 0) return "";
            int valueStart = data.indexOf(":", tokenStart) + 1;
            int quoteStart = data.indexOf("\"", valueStart) + 1;
            int quoteEnd = data.indexOf("\"", quoteStart);
            if (quoteStart > 0 && quoteEnd > quoteStart) {
                return data.substring(quoteStart, quoteEnd);
            }
            return "";
        }

        /**
         * Extracts the denial reason from the SSE data payload.
         * Expected format: {"reason": "..."}
         */
        private String extractReason(String data) {
            if (data == null || data.isEmpty()) return "Request denied";
            int reasonStart = data.indexOf("\"reason\"");
            if (reasonStart < 0) return "Request denied";
            int valueStart = data.indexOf(":", reasonStart) + 1;
            int quoteStart = data.indexOf("\"", valueStart) + 1;
            int quoteEnd = data.indexOf("\"", quoteStart);
            if (quoteStart > 0 && quoteEnd > quoteStart) {
                return data.substring(quoteStart, quoteEnd);
            }
            return "Request denied";
        }
    }
}
