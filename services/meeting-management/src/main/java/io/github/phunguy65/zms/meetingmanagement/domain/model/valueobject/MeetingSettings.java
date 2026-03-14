package io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject;

import io.github.phunguy65.zms.shared.domain.ValueObject;

/**
 * Meeting room settings stored as JSONB in the database.
 *
 * <p>Mapped via {@code @JdbcTypeCode(SqlTypes.JSON)} on the JPA entity.
 */
public record MeetingSettings(boolean waitingRoom, boolean allowGuest, boolean muteOnEntry)
        implements ValueObject {

    /** Default settings: waiting room on, guests allowed, mute on entry off. */
    public static MeetingSettings defaults() {
        return new MeetingSettings(true, true, false);
    }
}
