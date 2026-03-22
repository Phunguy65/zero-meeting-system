package io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject;

import io.github.phunguy65.zms.shared.domain.ValueObject;

/**
 * LiveKit permission grants to apply to a participant mid-session.
 *
 * <p>Used with {@code LiveKitPort#updateParticipantPermissions} to promote or demote
 * a participant without disconnecting them (e.g., raise-hand → speaker).
 *
 * @param canPublish     allow participant to publish audio/video tracks
 * @param canPublishData allow participant to send data messages (chat, reactions)
 * @param canSubscribe   allow participant to receive tracks from others
 */
public record ParticipantGrants(boolean canPublish, boolean canPublishData, boolean canSubscribe)
        implements ValueObject {

    /** Grants for a promoted speaker: full publish rights. */
    public static ParticipantGrants speaker() {
        return new ParticipantGrants(true, true, true);
    }

    /** Grants for a demoted viewer: subscribe and chat only, no media publish. */
    public static ParticipantGrants viewer() {
        return new ParticipantGrants(false, true, true);
    }

    /** Grants for a fully muted observer: subscribe only, no publish of any kind. */
    public static ParticipantGrants observer() {
        return new ParticipantGrants(false, false, true);
    }
}
