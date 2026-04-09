package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetMeetingQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingSettingsResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTitle;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMeetingUseCase {

    private final MeetingRepository meetingRepository;

    public GetMeetingUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @Transactional(readOnly = true)
    public Result<MeetingResponse, MeetingError> execute(GetMeetingQuery query) {
        return meetingRepository
                .findById(query.meetingId())
                .map(m -> Result.<MeetingResponse, MeetingError>success(toResponse(m)))
                .orElseGet(
                        () -> Result.failure(new MeetingError.MeetingNotFound(query.meetingId())));
    }

    private MeetingResponse toResponse(Meeting m) {
        return new MeetingResponse(
                m.getId().value(),
                m.getHostId().value(),
                m.getShortCode().value(),
                m.getTitle().map(MeetingTitle::value).orElse(null),
                m.getDescription().orElse(null),
                m.getStartTime().orElse(null),
                m.getEndTime().orElse(null),
                m.getType(),
                m.getStatus(),
                MeetingSettingsResponse.from(m.getSettings()),
                m.getCreatedAt());
    }
}
