package io.github.phunguy65.zms.meetingmanagement.application.query;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ListMeetingRecordingsQuery(
        UUID meetingId, int pageSize, @Nullable String pageTokenValue) {

    public ListMeetingRecordingsQuery {
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 100) pageSize = 100;
    }
}
