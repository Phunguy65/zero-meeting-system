package io.github.phunguy65.zms.meetingmanagement.application.query;

import io.github.phunguy65.zms.shared.domain.ScrollParams;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ListHostMeetingsQuery(
        UUID hostId, int pageSize, @Nullable String pageTokenValue) implements ScrollParams {

    public ListHostMeetingsQuery {
        if (pageSize < 1) pageSize = 1;
        if (pageSize > 100) pageSize = 100;
    }

    @Override
    public Optional<String> pageToken() {
        return Optional.ofNullable(pageTokenValue);
    }

    @Override
    public Optional<String> query() {
        return Optional.empty();
    }
}
