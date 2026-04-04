package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetRecordingQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.RecordingResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.port.RecordingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetRecordingUseCase {

    private final RecordingRepository recordingRepository;

    public GetRecordingUseCase(RecordingRepository recordingRepository) {
        this.recordingRepository = recordingRepository;
    }

    @Transactional(readOnly = true)
    public Result<RecordingResponse, MeetingError> execute(GetRecordingQuery query) {
        return recordingRepository
                .findById(query.recordingId())
                .map(r -> Result.<RecordingResponse, MeetingError>success(
                        StartRecordingUseCase.toResponse(r)))
                .orElseGet(() ->
                        Result.failure(new MeetingError.RecordingNotFound(query.recordingId())));
    }
}
