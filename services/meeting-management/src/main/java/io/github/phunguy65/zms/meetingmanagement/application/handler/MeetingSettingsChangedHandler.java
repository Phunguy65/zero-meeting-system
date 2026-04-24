package io.github.phunguy65.zms.meetingmanagement.application.handler;

import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingSettingsUpdatedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipationLog;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipantGrants;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Asynchronous handler for {@link MeetingSettingsUpdatedEvent}.
 *
 * <p>Reconciles LiveKit participant permissions after permission-relevant meeting settings
 * change on a LIVE meeting. Only PARTICIPANT sessions are updated; HOST and GUEST sessions
 * keep their fixed permission policies.
 *
 * <p>This handler:
 * <ul>
 *   <li>Runs asynchronously after the settings update transaction commits</li>
 *   <li>Filters for LIVE meetings only</li>
 *   <li>Filters for permission-relevant field changes only</li>
 *   <li>Uses best-effort processing: one failed update does not stop the rest</li>
 * </ul>
 */
@Component
public class MeetingSettingsChangedHandler {

    private static final Logger log = LoggerFactory.getLogger(MeetingSettingsChangedHandler.class);

    private final ParticipationLogRepository participationLogRepository;
    private final LiveKitPort liveKitPort;

    public MeetingSettingsChangedHandler(
            ParticipationLogRepository participationLogRepository, LiveKitPort liveKitPort) {
        this.participationLogRepository = participationLogRepository;
        this.liveKitPort = liveKitPort;
    }

    /**
     * Handles meeting settings updated events by synchronizing LiveKit permissions
     * for active PARTICIPANT sessions.
     *
     * @param event the settings updated event with old and new settings snapshots
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MeetingSettingsUpdatedEvent event) {
        // Only process LIVE meetings
        if (event.meetingStatus() != MeetingStatus.LIVE) {
            log.debug(
                    "Skipping permission sync for non-LIVE meeting {} (status: {})",
                    event.aggregateId(),
                    event.meetingStatus());
            return;
        }

        // Only process if permission-relevant fields changed
        if (!hasPermissionFieldsChanged(event.oldSettings(), event.newSettings())) {
            log.debug(
                    "Skipping permission sync for meeting {} - no permission fields changed",
                    event.aggregateId());
            return;
        }

        UUID meetingId = event.aggregateId();
        log.info("Starting permission sync for LIVE meeting {} after settings change", meetingId);

        syncActiveParticipantPermissions(meetingId, event.newSettings());
    }

    /**
     * Checks if any permission-relevant fields changed between old and new settings.
     */
    private boolean hasPermissionFieldsChanged(
            MeetingSettings oldSettings, MeetingSettings newSettings) {
        if (oldSettings == null || newSettings == null) {
            return true; // Treat missing settings as changed
        }
        return oldSettings.allowMicrophone() != newSettings.allowMicrophone()
                || oldSettings.allowVideo() != newSettings.allowVideo()
                || oldSettings.allowScreenShare() != newSettings.allowScreenShare()
                || oldSettings.chatEnabled() != newSettings.chatEnabled();
    }

    /**
     * Loads active sessions and updates permissions for PARTICIPANT roles only.
     * Uses best-effort processing: failures are logged but do not stop other updates.
     */
    private void syncActiveParticipantPermissions(UUID meetingId, MeetingSettings newSettings) {
        List<ParticipationLog> activeSessions =
                participationLogRepository.findActiveByMeetingId(meetingId);

        if (activeSessions.isEmpty()) {
            log.debug("No active sessions found for meeting {} during permission sync", meetingId);
            return;
        }

        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(MeetingId.of(meetingId));
        ParticipantGrants newGrants =
                ParticipantGrants.fromSettings(newSettings, ParticipantRole.PARTICIPANT);

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (ParticipationLog session : activeSessions) {
            ParticipantRole role = session.getRole();

            // Skip HOST and GUEST - their permissions are fixed
            if (role == ParticipantRole.HOST || role == ParticipantRole.GUEST) {
                skipped++;
                log.debug(
                        "Skipping {} session '{}' in meeting {} - fixed permissions",
                        role,
                        session.getLivekitIdentity().value(),
                        meetingId);
                continue;
            }

            // Update PARTICIPANT permissions
            String identity = session.getLivekitIdentity().value();
            var result = liveKitPort.updateParticipantPermissions(roomName, identity, newGrants);

            if (result instanceof Result.Success) {
                updated++;
                log.debug(
                        "Updated permissions for participant '{}' in meeting {}: canPublish={}, canPublishData={}",
                        identity,
                        meetingId,
                        newGrants.canPublish(),
                        newGrants.canPublishData());
            } else {
                failed++;
                log.warn(
                        "Failed to update permissions for participant '{}' in meeting {}: {}",
                        identity,
                        meetingId,
                        result);
            }
        }

        log.info(
                "Permission sync completed for meeting {}: updated={}, skipped={}, failed={}",
                meetingId,
                updated,
                skipped,
                failed);
    }
}
