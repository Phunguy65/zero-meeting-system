package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.LeaveMeetingCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitParticipantSid;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveMeetingUseCase {

    private final ParticipationLogRepository participationLogRepository;

    public LeaveMeetingUseCase(ParticipationLogRepository participationLogRepository) {
        this.participationLogRepository = participationLogRepository;
    }

    @Transactional
    public Result<Void, MeetingError> execute(LeaveMeetingCommand command) {
        var sid = LiveKitParticipantSid.of(command.livekitParticipantSid());
        var log = participationLogRepository.findActiveBySid(sid);
        if (log.isEmpty()) {
            return Result.failure(new MeetingError.ParticipationLogNotFound(
                    command.meetingId(), command.livekitParticipantSid()));
        }
        var entry = log.get();
        entry.leave(Instant.now());
        participationLogRepository.save(entry);
        return Result.success();
    }
}
