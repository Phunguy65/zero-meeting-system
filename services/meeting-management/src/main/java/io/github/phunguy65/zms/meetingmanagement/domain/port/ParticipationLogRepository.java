package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipationLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipationLogRepository {

    ParticipationLog save(ParticipationLog log);

    /**
     * Finds the most recent active (not yet left) participation log for a given meeting and device.
     * Used to record the departure time when a participant leaves.
     */
    Optional<ParticipationLog> findActiveByMeetingIdAndDeviceId(UUID meetingId, String deviceId);

    List<ParticipationLog> findByMeetingId(UUID meetingId);
}
