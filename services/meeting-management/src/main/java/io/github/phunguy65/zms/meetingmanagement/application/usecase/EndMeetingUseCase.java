package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.EndMeetingCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EndMeetingUseCase {

    private final MeetingRepository meetingRepository;
    private final LiveKitPort liveKitPort;
    private final ApplicationEventPublisher eventPublisher;

    public EndMeetingUseCase(
            MeetingRepository meetingRepository,
            LiveKitPort liveKitPort,
            ApplicationEventPublisher eventPublisher) {
        this.meetingRepository = meetingRepository;
        this.liveKitPort = liveKitPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, MeetingError> execute(EndMeetingCommand command) {
        var meeting = meetingRepository.findById(command.meetingId());
        if (meeting.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var m = meeting.get();

        if (!m.getHostId().equals(UserId.of(command.requesterId()))) {
            return Result.failure(new MeetingError.NotAuthorized(
                    command.requesterId(), m.getHostId().value()));
        }

        var endResult = m.end();
        if (endResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }

        var deleteResult = liveKitPort.deleteRoom(LiveKitRoomName.fromMeetingId(m.getId()));
        if (deleteResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }

        var saved = meetingRepository.save(m);
        saved.getDomainEvents().stream()
                .filter(e -> e instanceof PublishableEvent)
                .map(e -> (PublishableEvent) e)
                .forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();

        return Result.success();
    }
}
