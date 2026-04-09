package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetMeetingRecordingsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.RecordingResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.port.RecordingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.RecordingSummary;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMeetingRecordingsUseCase {

    private final RecordingRepository recordingRepository;

    public GetMeetingRecordingsUseCase(RecordingRepository recordingRepository) {
        this.recordingRepository = recordingRepository;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<RecordingResponse> execute(GetMeetingRecordingsQuery query) {
        var page = recordingRepository.findSummariesByMeetingId(
                query.meetingId(), query.cursor(), query.pageSize());
        var items = page.items().stream().map(this::toResponse).toList();
        return new CursorPageResponse<>(items, page.pageSize(), page.hasNext());
    }

    private RecordingResponse toResponse(RecordingSummary recording) {
        return new RecordingResponse(
                recording.id(),
                recording.meetingId(),
                recording.fileUrl(),
                recording.thumbnailUrl(),
                recording.status(),
                recording.startedAt(),
                recording.endedAt(),
                recording.durationSeconds(),
                recording.fileSizeBytes(),
                recording.createdAt());
    }
}
