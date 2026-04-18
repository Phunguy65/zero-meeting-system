package io.github.phunguy65.zms.presentation.main.history;

import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;

/** Sealed UI state for the meeting detail screen. */
public sealed interface MeetingDetailUiState {

    record Loading() implements MeetingDetailUiState {}

    record Success(MeetingHistoryDetail detail) implements MeetingDetailUiState {}

    record Error(String message) implements MeetingDetailUiState {}

    static MeetingDetailUiState loading() {
        return new Loading();
    }

    static MeetingDetailUiState success(MeetingHistoryDetail detail) {
        return new Success(detail);
    }

    static MeetingDetailUiState error(String message) {
        return new Error(message);
    }
}
