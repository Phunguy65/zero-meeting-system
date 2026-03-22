package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.ScrollParams;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository {

    Meeting save(Meeting meeting);

    Optional<Meeting> findById(UUID id);

    /** Finds a meeting by ID with a pessimistic write lock (SELECT FOR UPDATE). */
    Optional<Meeting> findByIdWithLock(UUID id);

    Optional<Meeting> findByShortCode(ShortCode shortCode);

    boolean existsByShortCode(ShortCode shortCode);

    CursorPageResponse<Meeting> findByHostId(UUID hostId, ScrollParams params);
}
