package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipatedMeetingDetailQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.InviteeResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingDetailResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingParticipantResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingSettingsResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.RecordingResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTitle;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingInviteeRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.RecordingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.InviteeSummary;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.ParticipantSummary;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.RecordingSummary;
import io.github.phunguy65.zms.shared.domain.Result;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetParticipatedMeetingDetailUseCase {

    private final MeetingRepository meetingRepository;
    private final ParticipationLogRepository participationLogRepository;
    private final RecordingRepository recordingRepository;
    private final MeetingInviteeRepository meetingInviteeRepository;

    public GetParticipatedMeetingDetailUseCase(
            MeetingRepository meetingRepository,
            ParticipationLogRepository participationLogRepository,
            RecordingRepository recordingRepository,
            MeetingInviteeRepository meetingInviteeRepository) {
        this.meetingRepository = meetingRepository;
        this.participationLogRepository = participationLogRepository;
        this.recordingRepository = recordingRepository;
        this.meetingInviteeRepository = meetingInviteeRepository;
    }

    @Transactional(readOnly = true)
    public Result<MeetingDetailResponse, MeetingError> execute(
            GetParticipatedMeetingDetailQuery query) {
        if (!query.requesterId().equals(query.userId())) {
            return Result.failure(new MeetingError.NotOwner(query.requesterId(), query.userId()));
        }
        if (!participationLogRepository.existsByMeetingIdAndUserId(
                query.meetingId(), query.userId())) {
            return Result.failure(
                    new MeetingError.NotParticipant(query.userId(), query.meetingId()));
        }

        return meetingRepository
                .findById(query.meetingId())
                .map(meeting -> toResponse(
                        meeting,
                        participationLogRepository.findDistinctParticipantSummariesByMeetingId(
                                meeting.getId().value()),
                        recordingRepository.findCompletedSummariesByMeetingId(
                                meeting.getId().value()),
                        meetingInviteeRepository.findSummariesByMeetingId(
                                meeting.getId().value())))
                .map(Result::<MeetingDetailResponse, MeetingError>success)
                .orElseGet(
                        () -> Result.failure(new MeetingError.MeetingNotFound(query.meetingId())));
    }

    private MeetingDetailResponse toResponse(
            Meeting meeting,
            List<ParticipantSummary> participants,
            List<RecordingSummary> recordings,
            List<InviteeSummary> invitees) {
        return new MeetingDetailResponse(
                meeting.getId().value(),
                meeting.getHostId().value(),
                meeting.getShortCode().value(),
                meeting.getTitle().map(MeetingTitle::value).orElse(null),
                meeting.getDescription().orElse(null),
                meeting.getStartTime().orElse(null),
                meeting.getEndTime().orElse(null),
                meeting.getType(),
                meeting.getStatus(),
                MeetingSettingsResponse.from(meeting.getSettings()),
                meeting.getCreatedAt(),
                participants.stream()
                        .map(MeetingParticipantResponse::fromProjection)
                        .toList(),
                recordings.stream()
                        .map(recording -> new RecordingResponse(
                                recording.id(),
                                recording.meetingId(),
                                recording.fileUrl(),
                                recording.thumbnailUrl(),
                                recording.status(),
                                recording.startedAt(),
                                recording.endedAt(),
                                recording.durationSeconds(),
                                recording.fileSizeBytes(),
                                recording.createdAt()))
                        .toList(),
                invitees.stream().map(InviteeResponse::fromProjection).toList());
    }
}
