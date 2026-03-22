package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.StopRecordingCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.RecordingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stops an active recording by deleting the LiveKit room's egress.
 *
 * <p>This use case does NOT transition the {@code Recording} aggregate state directly.
 * The state transition (RECORDING → COMPLETED or FAILED) is handled by the
 * {@code egress_ended} webhook handler once LiveKit confirms the egress has stopped.
 */
@Service
public class StopRecordingUseCase {

    private final MeetingRepository meetingRepository;
    private final RecordingRepository recordingRepository;
    private final LiveKitPort liveKitPort;

    public StopRecordingUseCase(
            MeetingRepository meetingRepository,
            RecordingRepository recordingRepository,
            LiveKitPort liveKitPort) {
        this.meetingRepository = meetingRepository;
        this.recordingRepository = recordingRepository;
        this.liveKitPort = liveKitPort;
    }

    @Transactional
    public Result<Void, MeetingError> execute(StopRecordingCommand command) {
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

        var recording = recordingRepository.findActiveByMeetingId(command.meetingId());
        if (recording.isEmpty()) {
            return Result.failure(new MeetingError.NoActiveRecording(command.meetingId()));
        }

        // Signal LiveKit to stop the egress; state transition happens via egress_ended webhook
        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(m.getId());
        var deleteResult = liveKitPort.deleteRoom(roomName);
        if (deleteResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }

        return Result.success();
    }
}
