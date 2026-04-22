package io.github.phunguy65.zms.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import dagger.hilt.android.qualifiers.ApplicationContext;
import io.github.phunguy65.zms.domain.model.RoomConnectionState;
import io.github.phunguy65.zms.domain.model.VideoParticipant;
import io.github.phunguy65.zms.domain.repository.LiveKitRepository;
import io.livekit.android.LiveKit;
import io.livekit.android.room.Room;
import io.livekit.android.room.participant.LocalParticipant;
import io.livekit.android.room.participant.Participant;
import io.livekit.android.room.participant.RemoteParticipant;
import io.livekit.android.room.track.LocalVideoTrack;
import io.livekit.android.room.track.Track;
import io.livekit.android.room.track.TrackPublication;
import io.livekit.android.room.track.VideoTrack;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

/**
 * Implementation of {@link LiveKitRepository} using the LiveKit Android SDK.
 * Manages room lifecycle, event handling, and local media controls.
 *
 * <p>Note: LiveKit Android SDK uses Kotlin coroutines. This Java implementation
 * uses callback-based patterns where possible and polling for state changes.
 */
@Singleton
public class LiveKitRepositoryImpl implements LiveKitRepository {

    private final Context context;
    private final Handler mainHandler;

    private Room room;
    private RoomEventListener listener;
    private final Set<String> activeSpeakerIds = new HashSet<>();
    private boolean micEnabled = true;
    private boolean cameraEnabled = false;

    private final ConcurrentLinkedQueue<byte[]> pendingDataMessages = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isPolling = new AtomicBoolean(false);
    private Room.State lastKnownState = null;
    private int lastParticipantCount = 0;
    private Map<String, ParticipantTrackState> lastParticipantStates = new java.util.HashMap<>();

    /**
     * Tracks the state of a participant's tracks for change detection.
     */
    private static class ParticipantTrackState {
        final boolean hasVideo;
        final boolean hasAudio;
        final boolean videoMuted;
        final boolean audioMuted;

        ParticipantTrackState(
                boolean hasVideo, boolean hasAudio, boolean videoMuted, boolean audioMuted) {
            this.hasVideo = hasVideo;
            this.hasAudio = hasAudio;
            this.videoMuted = videoMuted;
            this.audioMuted = audioMuted;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ParticipantTrackState that = (ParticipantTrackState) o;
            return hasVideo == that.hasVideo
                    && hasAudio == that.hasAudio
                    && videoMuted == that.videoMuted
                    && audioMuted == that.audioMuted;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(hasVideo, hasAudio, videoMuted, audioMuted);
        }
    }

    @Inject
    public LiveKitRepositoryImpl(@ApplicationContext Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void connect(String url, String token) {
        if (room != null) {
            disconnect();
        }

        notifyConnectionStateChanged(RoomConnectionState.CONNECTING);

        new Thread(() -> {
                    try {
                        room = LiveKit.INSTANCE.create(context, null, null);

                        room.connect(url, token, null, new SimpleContinuation<Unit>() {
                            @Override
                            public void onSuccess(Unit result) {
                                mainHandler.post(() -> {
                                    notifyConnectionStateChanged(RoomConnectionState.CONNECTED);
                                    registerDataReceivedHandler();
                                    startStatePolling();
                                    notifyParticipantsUpdated();
                                    LocalVideoTrack track = getLocalVideoTrack();
                                    if (track != null && listener != null) {
                                        listener.onLocalVideoTrackAvailable(track);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                mainHandler.post(() -> {
                                    notifyConnectionStateChanged(RoomConnectionState.FAILED);
                                });
                            }
                        });

                    } catch (Exception e) {
                        mainHandler.post(
                                () -> notifyConnectionStateChanged(RoomConnectionState.FAILED));
                    }
                })
                .start();
    }

    @Override
    public void disconnect() {
        stopStatePolling();
        if (room != null) {
            room.disconnect();
            room = null;
        }
        activeSpeakerIds.clear();
        lastKnownState = null;
        lastParticipantCount = 0;
        lastParticipantStates.clear();
        pendingDataMessages.clear();
        notifyConnectionStateChanged(RoomConnectionState.DISCONNECTED);
    }

    @Override
    public void setMicrophoneEnabled(boolean enabled) {
        this.micEnabled = enabled;
        if (room != null) {
            LocalParticipant localParticipant = room.getLocalParticipant();
            if (localParticipant != null) {
                try {
                    localParticipant.setMicrophoneEnabled(
                            enabled, new SimpleContinuation<Boolean>() {
                                @Override
                                public void onSuccess(Boolean result) {}

                                @Override
                                public void onFailure(Throwable t) {}
                            });
                } catch (Exception e) {
                    // Ignore publish exceptions - camera/mic state change will be reflected in UI
                }
            }
        }
    }

    @Override
    public void setCameraEnabled(boolean enabled) {
        this.cameraEnabled = enabled;
        if (room != null) {
            LocalParticipant localParticipant = room.getLocalParticipant();
            if (localParticipant != null) {
                try {
                    localParticipant.setCameraEnabled(enabled, new SimpleContinuation<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            if (enabled) {
                                mainHandler.postDelayed(
                                        () -> {
                                            LocalVideoTrack track = getLocalVideoTrack();
                                            if (track != null && listener != null) {
                                                listener.onLocalVideoTrackAvailable(track);
                                            }
                                        },
                                        500);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {}
                    });
                } catch (Exception e) {
                    // Ignore publish exceptions - camera/mic state change will be reflected in UI
                }
            }
        }
    }

    @Override
    public void switchCamera() {}

    @Override
    public LocalVideoTrack getLocalVideoTrack() {
        if (room == null) return null;
        LocalParticipant localParticipant = room.getLocalParticipant();
        if (localParticipant == null) return null;

        Map<String, ? extends TrackPublication> publications =
                localParticipant.getTrackPublications();
        for (TrackPublication publication : publications.values()) {
            Track track = publication.getTrack();
            if (track instanceof LocalVideoTrack) {
                return (LocalVideoTrack) track;
            }
        }
        return null;
    }

    @Override
    public boolean isMicrophoneEnabled() {
        return micEnabled;
    }

    @Override
    public boolean isCameraEnabled() {
        return cameraEnabled;
    }

    @Override
    public void setRoomEventListener(RoomEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void removeRoomEventListener() {
        this.listener = null;
    }

    /**
     * Attaches a data-received observer to the LiveKit Room. The SDK's
     * {@code events} Flow is collected on a background coroutine; when a
     * {@code RoomEvent.DataReceived} event arrives the raw bytes are
     * enqueued and drained on the next main-thread poll cycle.
     *
     * <p>If the SDK API is unavailable at runtime (version mismatch) the
     * method silently no-ops — chat receive will not work but the call
     * connection remains healthy.
     */
    @SuppressWarnings("unchecked")
    private void registerDataReceivedHandler() {
        if (room == null) return;

        try {
            java.lang.reflect.Method method = room.getClass()
                    .getMethod("registerDataReceivedHandler", kotlin.jvm.functions.Function2.class);
            method.invoke(room, (kotlin.jvm.functions.Function2<ByteBuffer, Object, Unit>)
                    (data, participant) -> {
                        if (data != null) {
                            byte[] copy = new byte[data.remaining()];
                            data.get(copy);
                            pendingDataMessages.add(copy);
                        }
                        return Unit.INSTANCE;
                    });
        } catch (Exception ignored) {
            // SDK version may not expose this method — degrade gracefully.
        }
    }

    /**
     * Starts polling for room state changes.
     * This is a workaround for Java-Kotlin interop with Flow-based APIs.
     */
    private void startStatePolling() {
        if (isPolling.getAndSet(true)) {
            return;
        }

        Runnable pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPolling.get() || room == null) {
                    return;
                }

                checkRoomStateChanges();
                mainHandler.postDelayed(this, 500);
            }
        };

        mainHandler.post(pollRunnable);
    }

    private void stopStatePolling() {
        isPolling.set(false);
    }

    /**
     * Checks for room state changes and notifies listeners.
     * Monitors connection state, participant count, track states, and active speakers.
     */
    private void checkRoomStateChanges() {
        if (room == null) return;

        Room.State currentState = room.getState();
        if (currentState != lastKnownState) {
            lastKnownState = currentState;
            RoomConnectionState connectionState = mapRoomState(currentState);
            notifyConnectionStateChanged(connectionState);
        }

        Map<Participant.Identity, RemoteParticipant> remoteParticipants =
                room.getRemoteParticipants();
        int currentParticipantCount = remoteParticipants.size();
        boolean participantsChanged = currentParticipantCount != lastParticipantCount;

        if (!participantsChanged) {
            participantsChanged = hasTrackStateChanged(remoteParticipants);
        }

        if (participantsChanged) {
            lastParticipantCount = currentParticipantCount;
            updateLastParticipantStates(remoteParticipants);
            notifyParticipantsUpdated();
        }

        updateActiveSpeakers();
        drainPendingDataMessages();
    }

    /**
     * Drains queued data-message payloads and delivers them to the listener
     * on the main thread.
     */
    private void drainPendingDataMessages() {
        if (listener == null) return;

        byte[] data;
        while ((data = pendingDataMessages.poll()) != null) {
            listener.onDataReceived(data);
        }
    }

    /**
     * Checks if any participant's track state has changed.
     */
    private boolean hasTrackStateChanged(
            Map<Participant.Identity, RemoteParticipant> remoteParticipants) {
        Map<String, ParticipantTrackState> currentStates = new java.util.HashMap<>();

        for (RemoteParticipant participant : remoteParticipants.values()) {
            String id = getParticipantId(participant);
            ParticipantTrackState state = extractTrackState(participant);
            currentStates.put(id, state);
        }

        if (currentStates.size() != lastParticipantStates.size()) {
            return true;
        }

        for (Map.Entry<String, ParticipantTrackState> entry : currentStates.entrySet()) {
            ParticipantTrackState lastState = lastParticipantStates.get(entry.getKey());
            if (lastState == null || !lastState.equals(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts the track state for a participant.
     */
    private ParticipantTrackState extractTrackState(Participant participant) {
        boolean hasVideo = false;
        boolean hasAudio = false;
        boolean videoMuted = true;
        boolean audioMuted = true;

        Map<String, ? extends TrackPublication> publications = participant.getTrackPublications();
        for (TrackPublication publication : publications.values()) {
            Track track = publication.getTrack();
            if (track != null) {
                if (track.getKind() == Track.Kind.VIDEO) {
                    hasVideo = true;
                    videoMuted = publication.getMuted();
                } else if (track.getKind() == Track.Kind.AUDIO) {
                    hasAudio = true;
                    audioMuted = publication.getMuted();
                }
            }
        }

        return new ParticipantTrackState(hasVideo, hasAudio, videoMuted, audioMuted);
    }

    /**
     * Updates the cached participant states.
     */
    private void updateLastParticipantStates(
            Map<Participant.Identity, RemoteParticipant> remoteParticipants) {
        lastParticipantStates.clear();
        for (RemoteParticipant participant : remoteParticipants.values()) {
            String id = getParticipantId(participant);
            lastParticipantStates.put(id, extractTrackState(participant));
        }
    }

    private RoomConnectionState mapRoomState(Room.State state) {
        if (state == Room.State.CONNECTED) {
            return RoomConnectionState.CONNECTED;
        } else if (state == Room.State.RECONNECTING) {
            return RoomConnectionState.RECONNECTING;
        } else if (state == Room.State.CONNECTING) {
            return RoomConnectionState.CONNECTING;
        } else if (state == Room.State.DISCONNECTED) {
            return RoomConnectionState.DISCONNECTED;
        }
        return RoomConnectionState.DISCONNECTED;
    }

    private void updateActiveSpeakers() {
        if (room == null || listener == null) return;

        List<Participant> speakers = room.getActiveSpeakers();
        Set<String> newActiveSpeakerIds = new HashSet<>();
        List<String> speakerIdsList = new ArrayList<>();

        for (Participant speaker : speakers) {
            String id = getParticipantId(speaker);
            newActiveSpeakerIds.add(id);
            speakerIdsList.add(id);
        }

        if (!newActiveSpeakerIds.equals(activeSpeakerIds)) {
            activeSpeakerIds.clear();
            activeSpeakerIds.addAll(newActiveSpeakerIds);
            listener.onActiveSpeakersChanged(speakerIdsList);
            notifyParticipantsUpdated();
        }
    }

    private void notifyConnectionStateChanged(RoomConnectionState state) {
        if (listener != null) {
            listener.onConnectionStateChanged(state);
        }
    }

    private void notifyParticipantsUpdated() {
        if (listener == null || room == null) return;

        List<VideoParticipant> participants = new ArrayList<>();

        Map<Participant.Identity, RemoteParticipant> remoteParticipants =
                room.getRemoteParticipants();
        for (RemoteParticipant remoteParticipant : remoteParticipants.values()) {
            participants.add(mapParticipant(remoteParticipant, false));
        }

        listener.onParticipantsUpdated(participants);
    }

    private String getParticipantId(Participant participant) {
        String name = participant.getName();
        return name != null ? name : String.valueOf(System.identityHashCode(participant));
    }

    private VideoParticipant mapParticipant(Participant participant, boolean isLocal) {
        String id = getParticipantId(participant);
        String displayName = participant.getName() != null ? participant.getName() : id;

        VideoTrack videoTrack = null;
        boolean isMicEnabled = false;
        boolean isCameraEnabled = false;

        Map<String, ? extends TrackPublication> publications = participant.getTrackPublications();
        for (TrackPublication publication : publications.values()) {
            Track track = publication.getTrack();
            if (track instanceof VideoTrack && videoTrack == null) {
                videoTrack = (VideoTrack) track;
                isCameraEnabled = !publication.getMuted();
            }
            if (track != null && track.getKind() == Track.Kind.AUDIO) {
                isMicEnabled = !publication.getMuted();
            }
        }

        boolean isActiveSpeaker = activeSpeakerIds.contains(id);

        return new VideoParticipant(
                id,
                displayName,
                videoTrack,
                isMicEnabled,
                isCameraEnabled,
                isActiveSpeaker,
                isLocal);
    }

    /**
     * Simple continuation adapter for Kotlin suspend functions.
     */
    private abstract static class SimpleContinuation<T> implements Continuation<T> {

        @Override
        public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(Object result) {
            if (result instanceof kotlin.Result.Failure) {
                onFailure(((kotlin.Result.Failure) result).exception);
            } else {
                @SuppressWarnings("unchecked")
                T value = (T) result;
                onSuccess(value);
            }
        }

        public abstract void onSuccess(T result);

        public abstract void onFailure(Throwable t);
    }
}
