package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.UpdateMeetingSettingsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.helper.PendingJoinRequestApprover;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingSettingsResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.PublishableEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingLimitsPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Duration;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Patches meeting settings for a SCHEDULED or LIVE meeting.
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Only the host may update settings.</li>
 *   <li>Only SCHEDULED and LIVE meetings may have their settings changed.</li>
 *   <li>{@code maxParticipants} cannot be changed when {@code admissionPolicy} is or
 *       becomes {@code ALLOW_ALL}.</li>
 *   <li>{@code maxParticipants} must not exceed the system ceiling.</li>
 *   <li>When a LIVE meeting's policy transitions to {@code ALLOW_ALL} or {@code allowGuest}
 *       transitions from {@code false} to {@code true}, all pending join requests are
 *       automatically approved.</li>
 * </ul>
 */
@Service
public class UpdateMeetingSettingsUseCase {

    private final MeetingRepository meetingRepository;
    private final MeetingLimitsPort limitsConfig;
    private final PendingJoinRequestApprover pendingJoinRequestApprover;
    private final ApplicationEventPublisher eventPublisher;

    public UpdateMeetingSettingsUseCase(
            MeetingRepository meetingRepository,
            MeetingLimitsPort limitsConfig,
            PendingJoinRequestApprover pendingJoinRequestApprover,
            ApplicationEventPublisher eventPublisher) {
        this.meetingRepository = meetingRepository;
        this.limitsConfig = limitsConfig;
        this.pendingJoinRequestApprover = pendingJoinRequestApprover;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<MeetingSettingsResponse, MeetingError> execute(
            UpdateMeetingSettingsCommand command) {

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

        // Resolve the effective admissionPolicy after patch
        AdmissionPolicy newPolicy = command.admissionPolicy().isPresent()
                ? AdmissionPolicy.valueOf(command.admissionPolicy().get())
                : existing.admissionPolicy();

        // maxParticipants cannot be changed when admissionPolicy is or becomes ALLOW_ALL
        if (newPolicy == AdmissionPolicy.ALLOW_ALL && command.maxParticipants().isPresent()) {
            return Result.failure(new MeetingError.InvalidSettings(
                    "maxParticipants cannot be changed when admissionPolicy is ALLOW_ALL"));
        }

        // Resolve maxParticipants and validate against ceiling
        int newMaxParticipants = command.maxParticipants().isPresent()
                ? command.maxParticipants().get()
                : existing.maxParticipants();

        if (newMaxParticipants > limitsConfig.getMaxParticipantsCeiling()) {
            return Result.failure(new MeetingError.InvalidSettings(
                    "maxParticipants " + newMaxParticipants + " exceeds system ceiling "
                            + limitsConfig.getMaxParticipantsCeiling()));
        }

        // Resolve joinRequestTimeout — null present = clear, undefined = keep
        Duration newJoinRequestTimeout;
        JsonNullable<Integer> timeoutPatch = command.joinRequestTimeoutSeconds();
        if (timeoutPatch == null || !timeoutPatch.isPresent()) {
            newJoinRequestTimeout = existing.joinRequestTimeout();
        } else if (timeoutPatch.get() == null) {
            newJoinRequestTimeout = null;
        } else {
            newJoinRequestTimeout = Duration.ofSeconds(timeoutPatch.get());
        }

        // Build merged settings (retain passwordHash — not patchable via this endpoint)
        MeetingSettings mergedSettings = new MeetingSettings(
                newPolicy,
                newJoinRequestTimeout,
                command.allowGuest().isPresent()
                        ? command.allowGuest().get()
                        : existing.allowGuest(),
                command.muteOnEntry().isPresent()
                        ? command.muteOnEntry().get()
                        : existing.muteOnEntry(),
                newMaxParticipants,
                command.recordingEnabled().isPresent()
                        ? command.recordingEnabled().get()
                        : existing.recordingEnabled(),
                command.screenShareMode().isPresent()
                        ? command.screenShareMode().get()
                        : existing.screenShareMode(),
                command.chatEnabled().isPresent()
                        ? command.chatEnabled().get()
                        : existing.chatEnabled(),
                existing.passwordHash());

        var updateResult = meeting.updateSettings(mergedSettings, command.requesterId());
        if (updateResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }

        var saved = meetingRepository.save(meeting);

        if (saved.getStatus() == MeetingStatus.LIVE) {
            boolean policyOpenedUp = newPolicy == AdmissionPolicy.ALLOW_ALL
                    && existing.admissionPolicy() != AdmissionPolicy.ALLOW_ALL;
            boolean guestOpenedUp = mergedSettings.allowGuest() && !existing.allowGuest();

            if (policyOpenedUp || guestOpenedUp) {
                pendingJoinRequestApprover.approveAll(saved, command.requesterId());
            }
        }

        saved.getDomainEvents().stream()
                .filter(e -> e instanceof PublishableEvent)
                .map(e -> (PublishableEvent) e)
                .forEach(eventPublisher::publishEvent);
        saved.clearDomainEvents();

        return Result.success(MeetingSettingsResponse.from(saved.getSettings()));
    }
}
