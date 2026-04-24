package io.github.phunguy65.zms.data.remote.sse;

import android.os.Handler;
import android.os.Looper;
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
 * SSE client for subscribing to host-side meeting events.
 *
 * <p>Handles join_request_created, join_request_expired, and participant_kicked event types.
 * Automatically retries failed connections up to {@value #MAX_RETRIES} times using
 * exponential back-off (1s, 2s, 4s). Retries are suppressed after explicit cancellation.
 */
public class MeetingEventSseClient {

    private static final String SSE_PATH = "/api/v1/meetings/%s/events";
    private static final int SSE_TIMEOUT_MINUTES = 10;
    private static final int MAX_RETRIES = 3;
    private static final int BASE_RETRY_DELAY_MS = 1000;

    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    private EventSource currentEventSource;
    private MeetingEventListener currentListener;

    private volatile boolean cancelled;
    private int retryCount;
    private String savedMeetingId;
    private String savedAuthToken;
    private MeetingEventListener savedListener;

    @Inject
    public MeetingEventSseClient(OkHttpClient httpClient) {
        this.httpClient = httpClient
                .newBuilder()
                .readTimeout(SSE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Subscribes to meeting events for the given meeting.
     *
     * @param meetingId the meeting ID
     * @param authToken the authentication token
     * @param listener  the listener to receive events
     */
    public void subscribe(String meetingId, String authToken, MeetingEventListener listener) {
        cancel();

        this.cancelled = false;
        this.retryCount = 0;
        this.savedMeetingId = meetingId;
        this.savedAuthToken = authToken;
        this.savedListener = listener;
        this.currentListener = listener;

        openEventSource(meetingId, authToken);
    }

    /**
     * Cancels the current SSE subscription.
     */
    public void cancel() {
        cancelled = true;
        if (currentEventSource != null) {
            currentEventSource.cancel();
            currentEventSource = null;
        }
        currentListener = null;
    }

    private void openEventSource(String meetingId, String authToken) {
        String url = BuildConfig.API_BASE_URL + String.format(SSE_PATH, meetingId);
        Request.Builder requestBuilder =
                new Request.Builder().url(url).header("Accept", "text/event-stream");

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        EventSource.Factory factory = EventSources.createFactory(httpClient);
        currentEventSource = factory.newEventSource(requestBuilder.build(), new SseEventListener());
    }

    private void scheduleRetry() {
        if (cancelled || retryCount >= MAX_RETRIES) {
            if (!cancelled && currentListener != null) {
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
                    if (!cancelled && savedListener != null) {
                        currentListener = savedListener;
                        openEventSource(savedMeetingId, savedAuthToken);
                    }
                },
                delayMs);
    }

    private class SseEventListener extends EventSourceListener {

        @Override
        public void onOpen(EventSource eventSource, Response response) {
            if (currentListener != null) {
                mainHandler.post(() -> {
                    if (currentListener != null) {
                        currentListener.onConnected();
                    }
                });
            }
        }

        @Override
        public void onEvent(EventSource eventSource, String id, String type, String data) {
            if (currentListener == null) return;

            mainHandler.post(() -> {
                if (currentListener == null) return;

                switch (type) {
                    case "join_request_created":
                        String requestId = extractField(data, "requestId");
                        String meetingId = extractField(data, "meetingId");
                        String displayName = extractField(data, "displayName");
                        currentListener.onJoinRequestCreated(requestId, meetingId, displayName);
                        break;

                    case "join_request_expired":
                        String expiredRequestId = extractField(data, "requestId");
                        currentListener.onJoinRequestExpired(expiredRequestId);
                        break;

                    case "participant_kicked":
                        String kickedMeetingId = extractField(data, "meetingId");
                        String kickedUserId = extractField(data, "userId");
                        String kickedDisplayName = extractField(data, "displayName");
                        currentListener.onParticipantKicked(
                                kickedMeetingId, kickedUserId, kickedDisplayName);
                        break;

                    default:
                        break;
                }
            });
        }

        @Override
        public void onClosed(EventSource eventSource) {
            if (currentListener != null) {
                mainHandler.post(() -> {
                    if (currentListener != null) {
                        currentListener.onDisconnected();
                    }
                });
            }
        }

        @Override
        public void onFailure(EventSource eventSource, Throwable t, Response response) {
            if (cancelled || currentListener == null) return;

            scheduleRetry();
        }

        private String extractField(String data, String fieldName) {
            if (data == null || data.isEmpty()) return "";
            String searchKey = "\"" + fieldName + "\"";
            int fieldStart = data.indexOf(searchKey);
            if (fieldStart < 0) return "";
            int valueStart = data.indexOf(":", fieldStart) + 1;
            int quoteStart = data.indexOf("\"", valueStart) + 1;
            int quoteEnd = data.indexOf("\"", quoteStart);
            if (quoteStart > 0 && quoteEnd > quoteStart) {
                return data.substring(quoteStart, quoteEnd);
            }
            return "";
        }
    }

    /**
     * Listener for host-side meeting events with typed callbacks.
     */
    public interface MeetingEventListener {

        void onConnected();

        void onDisconnected();

        void onJoinRequestCreated(String requestId, String meetingId, String displayName);

        void onJoinRequestExpired(String requestId);

        void onParticipantKicked(String meetingId, String kickedUserId, String displayName);

        void onError(String message);
    }
}
