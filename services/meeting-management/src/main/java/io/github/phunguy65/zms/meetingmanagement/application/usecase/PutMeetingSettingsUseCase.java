package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.PutMeetingSettingsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.helper.MeetingSettingsPasswordResolver;
import io.github.phunguy65.zms.meetingmanagement.application.helper.PendingJoinRequestApprover;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingSettingsResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingLimitsPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PutMeetingSettingsUseCase {

    private final MeetingRepository meetingRepository;
    private final MeetingLimitsPort limitsConfig;
    private final PendingJoinRequestApprover pendingJoinRequestApprover;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordHasher passwordHasher;

    public PutMeetingSettingsUseCase(
            MeetingRepository meetingRepository,
            MeetingLimitsPort limitsConfig,
            PendingJoinRequestApprover pendingJoinRequestApprover,
            ApplicationEventPublisher eventPublisher,
            PasswordHasher passwordHasher) {
        this.meetingRepository = meetingRepository;
        this.limitsConfig = limitsConfig;
        this.pendingJoinRequestApprover = pendingJoinRequestApprover;
        this.eventPublisher = eventPublisher;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public Result<MeetingSettingsResponse, MeetingError> execute(
            PutMeetingSettingsCommand command) {
        var meetingOpt = meetingRepository.findByIdWithLock(command.meetingId());
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var meeting = meetingOpt.get();

        if (!meeting.getHostId().equals(UserId.of(command.requesterId()))) {
            return Result.failure(new MeetingError.NotAuthorized(
                    command.requesterId(), meeting.getHostId().value()));
        }

        if (meeting.getStatus() != MeetingStatus.SCHEDULED
                && meeting.getStatus() != MeetingStatus.LIVE) {
            return Result.failure(new MeetingError.InvalidStatusTransition(
                    meeting.getStatus(), MeetingStatus.SCHEDULED));
        }

        MeetingSettings existing = meeting.getSettings();
        MeetingSettings requested = command.settings();

        if (requested.maxParticipants() > limitsConfig.getMaxParticipantsCeiling()) {
            return Result.failure(new MeetingError.InvalidSettings(
                    "maxParticipants " + requested.maxParticipants() + " exceeds system ceiling "
                            + limitsConfig.getMaxParticipantsCeiling()));
        }

        if (requested.admissionPolicy() == AdmissionPolicy.ALLOW_ALL
                && requested.maxParticipants() != existing.maxParticipants()) {
            return Result.failure(new MeetingError.InvalidSettings(
                    "maxParticipants cannot be changed when admissionPolicy is ALLOW_ALL"));
        }

        MeetingSettings replacement = MeetingSettingsPasswordResolver.withRawPassword(
                requested,
                MeetingSettingsPasswordResolver.normalizeRawPassword(command.rawPassword()),
                passwordHasher);

        var updateResult = meeting.updateSettings(replacement, command.requesterId());
        if (updateResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }

        if (meeting.getStatus() == MeetingStatus.LIVE) {
            boolean policyOpenedUp = replacement.admissionPolicy() == AdmissionPolicy.ALLOW_ALL
                    && existing.admissionPolicy() != AdmissionPolicy.ALLOW_ALL;
            boolean guestOpenedUp = replacement.allowGuest() && !existing.allowGuest();
            if (policyOpenedUp || guestOpenedUp) {
                var approvalResult =
                        pendingJoinRequestApprover.approveAll(meeting, command.requesterId());
                if (approvalResult
                        instanceof Result.Failure<Integer, MeetingError>(MeetingError error)) {
                    return Result.failure(error);
                }
            }
        }

        var saved = meetingRepository.save(meeting);

        saved.getDomainEvents().stream()
                .filter(e -> e instanceof PublishableEvent)
                .map(e -> (PublishableEvent) e)
                .forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();

        return Result.success(MeetingSettingsResponse.from(saved.getSettings()));
    }
}
