package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.model.Recording;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordingRepository {

    Recording save(Recording recording);

    Optional<Recording> findById(UUID id);

    /** Returns the recording currently in RECORDING or PROCESSING state for the given meeting. */
    Optional<Recording> findActiveByMeetingId(UUID meetingId);

    List<Recording> findByMeetingId(UUID meetingId);
}
