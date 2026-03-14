package io.github.phunguy65.zms.meetingmanagement.domain.port;

import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.shared.domain.CursorPageResult;
import io.github.phunguy65.zms.shared.domain.ScrollParams;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository {

    Meeting save(Meeting meeting);

    Optional<Meeting> findById(UUID id);

    Optional<Meeting> findByShortCode(ShortCode shortCode);

    boolean existsByShortCode(ShortCode shortCode);

    CursorPageResult<Meeting> findByHostId(UUID hostId, ScrollParams params);
}
