package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.PutMeetingSettingsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.helper.MeetingSettingsPasswordResolver;
import io.github.phunguy65.zms.meetingmanagement.application.helper.PendingJoinRequestApprover;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingSettingsResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingInviteTokensInvalidatedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingInviteTokensInvalidatedEvent.AffectedInviteeInfo;
import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingInvitee;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.InviteeDisplayName;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTitle;
import io.github.phunguy65.zms.meetingmanagement.domain.port.InviteTokenRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingInviteeRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingLimitsPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
    private final InviteTokenRepository inviteTokenRepository;
    private final MeetingInviteeRepository meetingInviteeRepository;

    public PutMeetingSettingsUseCase(
            MeetingRepository meetingRepository,
            MeetingLimitsPort limitsConfig,
            PendingJoinRequestApprover pendingJoinRequestApprover,
            ApplicationEventPublisher eventPublisher,
            PasswordHasher passwordHasher,
            InviteTokenRepository inviteTokenRepository,
            MeetingInviteeRepository meetingInviteeRepository) {
        this.meetingRepository = meetingRepository;
        this.limitsConfig = limitsConfig;
        this.pendingJoinRequestApprover = pendingJoinRequestApprover;
        this.eventPublisher = eventPublisher;
        this.passwordHasher = passwordHasher;
        this.inviteTokenRepository = inviteTokenRepository;
        this.meetingInviteeRepository = meetingInviteeRepository;
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

        int invalidatedInviteCount = 0;
        if (meeting.getStatus() == MeetingStatus.SCHEDULED
                && passwordChanged(existing, replacement)) {
            invalidatedInviteCount = revokeTokensAndPublishEvent(
                    saved.getId().value(),
                    saved.getHostId().value(),
                    saved.getTitle().map(MeetingTitle::value).orElse(null),
                    saved.getShortCode().value());
        }

        saved.getDomainEvents().stream()
                .filter(e -> e instanceof PublishableEvent)
                .map(e -> (PublishableEvent) e)
                .forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();

        return Result.success(
                MeetingSettingsResponse.from(saved.getSettings(), invalidatedInviteCount));
    }

    private boolean passwordChanged(MeetingSettings existing, MeetingSettings replacement) {
        return !Objects.equals(existing.password(), replacement.password());
    }

    private int revokeTokensAndPublishEvent(
            UUID meetingId, UUID hostId, String meetingTitle, String shortCode) {
        int revokedCount = inviteTokenRepository.revokeAllPendingByMeetingId(meetingId);

        List<MeetingInvitee> invitees = meetingInviteeRepository.findByMeetingId(meetingId);
        List<AffectedInviteeInfo> affectedInvitees = invitees.stream()
                .map(invitee -> new AffectedInviteeInfo(
                        invitee.getId().value(),
                        invitee.getUserId().map(UserId::value).orElse(null),
                        invitee.getEmail().value(),
                        invitee.getDisplayName().map(InviteeDisplayName::value).orElse(null)))
                .toList();

        eventPublisher.publishEvent(new MeetingInviteTokensInvalidatedEvent(
                UUID.randomUUID(),
                meetingId,
                hostId,
                meetingTitle,
                shortCode,
                affectedInvitees,
                Instant.now()));

        return revokedCount;
    }
}
