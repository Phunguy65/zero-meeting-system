package io.github.phunguy65.zms.domain.model;

/**
 * Represents the connection state of a LiveKit room.
 */
public enum RoomConnectionState {
    /** Not connected to any room. */
    DISCONNECTED,

    /** Currently attempting to connect to a room. */
    CONNECTING,

    /** Successfully connected to the room. */
    CONNECTED,

    /** Connection lost, attempting to reconnect. */
    RECONNECTING,

    /** Connection failed after exhausting retry attempts. */
    FAILED
}
