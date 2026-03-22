package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.StartMeetingCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StartMeetingUseCase {

    private final MeetingRepository meetingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StartMeetingUseCase(
            MeetingRepository meetingRepository, ApplicationEventPublisher eventPublisher) {
        this.meetingRepository = meetingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, MeetingError> execute(StartMeetingCommand command) {
        var meeting = meetingRepository.findById(command.meetingId());
        if (meeting.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var m = meeting.get();

        if (!m.getHostId().equals(UserId.of(command.requesterId()))) {
            return Result.failure(new MeetingError.NotAuthorized(
                    command.requesterId(), m.getHostId().value()));
        }

        var startResult = m.start();
        if (startResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
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
