package io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.shared.domain.ValueObject;
import org.jspecify.annotations.Nullable;

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

    /**
     * Derives LiveKit permission grants from meeting settings and participant role.
     *
     * <p>Permission policy:
     * <ul>
     *   <li>HOST — full permissions regardless of meeting settings</li>
     *   <li>GUEST — subscribe-only regardless of meeting settings</li>
     *   <li>PARTICIPANT — derived from meeting settings:
     *     <ul>
     *       <li>{@code canPublish} = true if any media source is enabled
     *           (microphone OR video OR screen share)</li>
     *       <li>{@code canPublishData} = {@code chatEnabled}</li>
     *       <li>{@code canSubscribe} = always true</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param settings meeting settings (may be null for backwards compatibility)
     * @param role     participant role
     * @return computed grants for the role
     */
    public static ParticipantGrants fromSettings(
            @Nullable MeetingSettings settings, ParticipantRole role) {
        return switch (role) {
            case HOST -> speaker();
            case GUEST -> observer();
            case PARTICIPANT -> {
                if (settings == null) {
                    // Fallback for backwards compatibility — full permissions
                    yield speaker();
                }
                boolean anyMediaSourceEnabled = settings.allowMicrophone()
                        || settings.allowVideo()
                        || settings.allowScreenShare();
                boolean canPublish = anyMediaSourceEnabled;
                boolean canPublishData = settings.chatEnabled();
                yield new ParticipantGrants(canPublish, canPublishData, true);
            }
        };
    }

    /**
     * Grants for a promoted speaker: full publish rights.
     */
    public static ParticipantGrants speaker() {
        return new ParticipantGrants(true, true, true);
    }

    /**
     * Grants for a demoted viewer: subscribe and chat only, no media publish.
     */
    public static ParticipantGrants viewer() {
        return new ParticipantGrants(false, true, true);
    }

    /**
     * Grants for a fully muted observer: subscribe only, no publish of any kind.
     */
    public static ParticipantGrants observer() {
        return new ParticipantGrants(false, false, true);
    }
}
