package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipantsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.ParticipantListItemResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetParticipantsUseCase {

    private final MeetingRepository meetingRepository;
    private final ParticipationLogRepository participationLogRepository;

    public GetParticipantsUseCase(
            MeetingRepository meetingRepository,
            ParticipationLogRepository participationLogRepository) {
        this.meetingRepository = meetingRepository;
        this.participationLogRepository = participationLogRepository;
    }

    @Transactional(readOnly = true)
    public Result<List<ParticipantListItemResponse>, MeetingError> execute(
            GetParticipantsQuery query) {
        if (meetingRepository.findById(query.meetingId()).isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(query.meetingId()));
        }

        return Result.success(
                participationLogRepository
                        .findParticipantSummariesByMeetingId(query.meetingId())
                        .stream()
                        .map(ParticipantListItemResponse::fromProjection)
                        .toList());
    }
}
