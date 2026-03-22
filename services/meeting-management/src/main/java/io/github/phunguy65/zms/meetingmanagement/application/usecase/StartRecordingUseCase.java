package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.StartRecordingCommand;
import io.github.phunguy65.zms.meetingmanagement.application.response.RecordingResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.Recording;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.RecordingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StartRecordingUseCase {

    private final MeetingRepository meetingRepository;
    private final RecordingRepository recordingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StartRecordingUseCase(
            MeetingRepository meetingRepository,
            RecordingRepository recordingRepository,
            ApplicationEventPublisher eventPublisher) {
        this.meetingRepository = meetingRepository;
        this.recordingRepository = recordingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<RecordingResponse, MeetingError> execute(StartRecordingCommand command) {
        var meeting = meetingRepository.findById(command.meetingId());
        if (meeting.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var m = meeting.get();

        if (!m.getHostId().equals(UserId.of(command.requesterId()))) {
            return Result.failure(new MeetingError.NotAuthorized(
                    command.requesterId(), m.getHostId().value()));
        }

        if (m.getStatus() != MeetingStatus.LIVE) {
            return Result.failure(
                    new MeetingError.InvalidStatusTransition(m.getStatus(), MeetingStatus.LIVE));
        }

        // Check no active (PENDING or RECORDING) recording already exists
        if (recordingRepository.findActiveByMeetingId(command.meetingId()).isPresent()) {
            return Result.failure(new MeetingError.RecordingAlreadyActive(command.meetingId()));
        }

        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(m.getId());
        Recording recording = Recording.startFor(MeetingId.of(command.meetingId()), roomName);
        Recording saved = recordingRepository.save(recording);

        saved.getDomainEvents().stream()
                .filter(e -> e instanceof PublishableEvent)
                .map(e -> (PublishableEvent) e)
                .forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();

        return Result.success(toResponse(saved));
    }

    static RecordingResponse toResponse(Recording r) {
        return new RecordingResponse(
                r.getId(),
                r.getMeetingId().value(),
                r.getFileUrl().orElse(null),
                r.getThumbnailUrl().orElse(null),
                r.getStatus(),
                r.getStartedAt(),
                r.getEndedAt().orElse(null),
                r.getDurationSeconds(),
                r.getFileSizeBytes(),
                r.getCreatedAt());
    }
}
