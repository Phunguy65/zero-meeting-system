package io.github.phunguy65.zms.meetingmanagement.application.query;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record GetParticipantsQuery(
        UUID meetingId, int pageSize, @Nullable String pageTokenValue) {

    public GetParticipantsQuery {
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 100) pageSize = 100;
    }
}
