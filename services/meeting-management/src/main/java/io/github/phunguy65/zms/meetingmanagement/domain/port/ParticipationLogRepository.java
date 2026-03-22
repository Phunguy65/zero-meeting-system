package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipationLog;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitParticipantSid;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogCursor;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipationLogRepository {

    ParticipationLog save(ParticipationLog log);

    /**
     * Finds the active (not yet left) session by LiveKit participant SID.
     * Primary lookup path for {@code participant_left} webhook handler.
     */
    Optional<ParticipationLog> findActiveBySid(LiveKitParticipantSid sid);

    /**
     * Finds the active (not yet left) session by meeting and LiveKit identity.
     * Used by {@code participant_joined} webhook handler to assign the SID.
     */
    Optional<ParticipationLog> findActiveByMeetingIdAndIdentity(
            UUID meetingId, LiveKitIdentity identity);

    List<ParticipationLog> findByMeetingId(UUID meetingId);

    /** Returns the count of currently active (not yet left) participants for a meeting. */
    long countActiveByMeetingId(UUID meetingId);

    /** Keyset-scroll participation logs for a meeting, ordered by (joined_at DESC, id DESC). */
    CursorPageResponse<ParticipationLog> findByMeetingIdKeyset(
            UUID meetingId, ParticipationLogCursor cursor, int pageSize);
}
