package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.ListMeetingRecordingsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.RecordingResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.port.RecordingRepository;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListMeetingRecordingsUseCase {

    private final RecordingRepository recordingRepository;

    public ListMeetingRecordingsUseCase(RecordingRepository recordingRepository) {
        this.recordingRepository = recordingRepository;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<RecordingResponse> execute(
            ListMeetingRecordingsQuery query, ScrollCursor cursor) {
        var page = recordingRepository.findByMeetingIdKeyset(
                query.meetingId(), cursor, query.pageSize());
        var items = page.items().stream().map(StartRecordingUseCase::toResponse).toList();
        return new CursorPageResponse<>(items, page.pageSize(), page.hasNext());
    }
}
