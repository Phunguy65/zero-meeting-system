package io.github.phunguy65.zms.domain.model;

import java.util.List;

/**
 * Paginated slice of {@link MeetingHistory} used by the meeting history list screen.
 *
 * <p>The backend returns a keyset-style cursor via {@code nextPageToken}. When {@code hasNext}
 * is false, {@code nextPageToken} is {@code null}.
 */
public record MeetingHistoryPage(List<MeetingHistory> items, String nextPageToken, boolean hasNext) {

    public MeetingHistoryPage {
        items = List.copyOf(items);
    }

    public static MeetingHistoryPage empty() {
        return new MeetingHistoryPage(List.of(), null, false);
    }
}
