package io.github.phunguy65.zms.presentation.main.history;

import io.github.phunguy65.zms.domain.model.MeetingHistory;
import java.util.List;

/**
 * Sealed UI state for the meeting history list screen.
 *
 * <p>Feature-specific state captures pagination metadata (next page token, has-more flag, and
 * in-flight pagination loads) which does not fit a generic {@code UiState<T>}.
 */
public sealed interface MeetingHistoryUiState {

    /** Initial load / full refresh in progress. Shows skeleton placeholders. */
    record Loading() implements MeetingHistoryUiState {}

    /**
     * Data loaded. May be empty (separate {@link Empty} is emitted only when list is empty AND we
     * are NOT refreshing/paginating).
     *
     * @param items loaded meetings so far
     * @param nextPageToken cursor for the next page (null when there is no more)
     * @param hasMore true if further pages exist
     * @param isLoadingMore true while fetching the next page
     * @param isRefreshing true while a pull-to-refresh is running
     */
    record Success(
            List<MeetingHistory> items,
            String nextPageToken,
            boolean hasMore,
            boolean isLoadingMore,
            boolean isRefreshing)
            implements MeetingHistoryUiState {

        public Success {
            items = List.copyOf(items);
        }
    }

    /** Loaded successfully but user has no history. */
    record Empty() implements MeetingHistoryUiState {}

    /** Initial load failed. Shown as a full-screen error state with Retry. */
    record Error(String message) implements MeetingHistoryUiState {}

    static MeetingHistoryUiState loading() {
        return new Loading();
    }

    static MeetingHistoryUiState empty() {
        return new Empty();
    }

    static MeetingHistoryUiState error(String message) {
        return new Error(message);
    }
}
