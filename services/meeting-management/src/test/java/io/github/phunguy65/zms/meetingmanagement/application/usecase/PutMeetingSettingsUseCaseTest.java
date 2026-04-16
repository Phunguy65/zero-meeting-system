package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.phunguy65.zms.meetingmanagement.application.command.PutMeetingSettingsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.helper.PendingJoinRequestApprover;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingSettingsResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingSettingsUpdatedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingType;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingLimitsPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PutMeetingSettingsUseCaseTest {

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    MeetingLimitsPort limitsConfig;

    @Mock
    PendingJoinRequestApprover pendingJoinRequestApprover;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @Mock
    PasswordHasher passwordHasher;

    PutMeetingSettingsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PutMeetingSettingsUseCase(
                meetingRepository,
                limitsConfig,
                pendingJoinRequestApprover,
                eventPublisher,
                passwordHasher);
    }

    @Test
    void execute_replacesSettingsAndPublishesEvent() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Meeting meeting = meetingWithStatus(
                meetingId,
                hostId,
                MeetingStatus.SCHEDULED,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        true,
                        30,
                        false,
                        "ALL",
                        false,
                        "old"));
        when(limitsConfig.getMaxParticipantsCeiling()).thenReturn(300);
        when(meetingRepository.findByIdWithLock(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordHasher.hash("secret-pass")).thenReturn("hashed-secret");

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        null,
                        true,
                        false,
                        40,
                        true,
                        "HOST_ONLY",
                        true,
                        null),
                "secret-pass"));

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = (MeetingSettingsResponse) ((Result.Success<?, ?>) result).value();
        assertThat(response.joinRequestTimeoutSeconds()).isNull();
        assertThat(response.allowGuest()).isTrue();
        assertThat(response.muteOnEntry()).isFalse();
        assertThat(response.maxParticipants()).isEqualTo(40);
        assertThat(response.screenShareMode()).isEqualTo("HOST_ONLY");
        assertThat(response.requirePassword()).isTrue();

        verify(passwordHasher).hash("secret-pass");
        verify(meetingRepository)
                .save(argThat(
                        saved -> "hashed-secret".equals(saved.getSettings().passwordHash())
                                && saved.getSettings().joinRequestTimeout() == null
                                && saved.getSettings().allowGuest()
                                && saved.getSettings().maxParticipants() == 40));
        ArgumentCaptor<MeetingSettingsUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(MeetingSettingsUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().aggregateId()).isEqualTo(meetingId);
        assertThat(eventCaptor.getValue().hostId()).isEqualTo(hostId);
        assertThat(eventCaptor.getValue().updatedBy()).isEqualTo(hostId);
        assertThat(eventCaptor.getValue().meetingStatus()).isEqualTo(MeetingStatus.SCHEDULED);
        verify(pendingJoinRequestApprover, never()).approveAll(any(), any());
    }

    @Test
    void execute_nullPassword_clearsStoredHash() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Meeting meeting = meetingWithStatus(
                meetingId,
                hostId,
                MeetingStatus.SCHEDULED,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        "old"));
        when(limitsConfig.getMaxParticipantsCeiling()).thenReturn(300);
        when(meetingRepository.findByIdWithLock(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null),
                null));

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = (MeetingSettingsResponse) ((Result.Success<?, ?>) result).value();
        assertThat(response.requirePassword()).isFalse();
        verify(meetingRepository)
                .save(argThat(saved -> saved.getSettings().passwordHash() == null));
        verifyNoInteractions(passwordHasher);
    }

    @Test
    void execute_nullTimeout_clearsExistingTimeout() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Meeting meeting = meetingWithStatus(
                meetingId,
                hostId,
                MeetingStatus.SCHEDULED,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null));
        when(limitsConfig.getMaxParticipantsCeiling()).thenReturn(300);
        when(meetingRepository.findByIdWithLock(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        null,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null),
                null));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(meetingRepository)
                .save(argThat(saved -> saved.getSettings().joinRequestTimeout() == null));
    }

    @Test
    void execute_returnsNotAuthorizedForNonHost() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(meetingRepository.findByIdWithLock(meetingId))
                .thenReturn(Optional.of(meetingWithStatus(
                        meetingId,
                        hostId,
                        MeetingStatus.SCHEDULED,
                        settings(
                                AdmissionPolicy.MANUAL_APPROVAL,
                                90,
                                false,
                                false,
                                30,
                                false,
                                "ALL",
                                true,
                                null))));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                requesterId,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null),
                null));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isEqualTo(new MeetingError.NotAuthorized(requesterId, hostId));
        verifyNoInteractions(passwordHasher, pendingJoinRequestApprover, eventPublisher);
    }

    @Test
    void execute_returnsInvalidStatusForEndedMeeting() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        when(meetingRepository.findByIdWithLock(meetingId))
                .thenReturn(Optional.of(meetingWithStatus(
                        meetingId,
                        hostId,
                        MeetingStatus.ENDED,
                        settings(
                                AdmissionPolicy.MANUAL_APPROVAL,
                                90,
                                false,
                                false,
                                30,
                                false,
                                "ALL",
                                true,
                                null))));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null),
                null));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isEqualTo(new MeetingError.InvalidStatusTransition(
                        MeetingStatus.ENDED, MeetingStatus.SCHEDULED));
        verifyNoInteractions(passwordHasher, pendingJoinRequestApprover, eventPublisher);
    }

    @Test
    void execute_rejectsMaxParticipantsAboveCeiling() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        when(limitsConfig.getMaxParticipantsCeiling()).thenReturn(50);
        when(meetingRepository.findByIdWithLock(meetingId))
                .thenReturn(Optional.of(meetingWithStatus(
                        meetingId,
                        hostId,
                        MeetingStatus.SCHEDULED,
                        settings(
                                AdmissionPolicy.MANUAL_APPROVAL,
                                90,
                                false,
                                false,
                                30,
                                false,
                                "ALL",
                                true,
                                null))));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        80,
                        false,
                        "ALL",
                        true,
                        null),
                null));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isInstanceOf(MeetingError.InvalidSettings.class);
        verify(meetingRepository, never()).save(any());
        verifyNoInteractions(pendingJoinRequestApprover, eventPublisher);
    }

    @Test
    void execute_rejectsAllowAllWhenMaxParticipantsChanges() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        when(limitsConfig.getMaxParticipantsCeiling()).thenReturn(300);
        when(meetingRepository.findByIdWithLock(meetingId))
                .thenReturn(Optional.of(meetingWithStatus(
                        meetingId,
                        hostId,
                        MeetingStatus.SCHEDULED,
                        settings(
                                AdmissionPolicy.MANUAL_APPROVAL,
                                90,
                                false,
                                false,
                                30,
                                false,
                                "ALL",
                                true,
                                null))));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(AdmissionPolicy.ALLOW_ALL, 90, false, false, 40, false, "ALL", true, null),
                null));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isInstanceOf(MeetingError.InvalidSettings.class);
        verify(meetingRepository, never()).save(any());
        verifyNoInteractions(pendingJoinRequestApprover, eventPublisher);
    }

    @Test
    void execute_livePolicyOpening_autoApprovesPendingRequests() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Meeting meeting = meetingWithStatus(
                meetingId,
                hostId,
                MeetingStatus.LIVE,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null));
        when(limitsConfig.getMaxParticipantsCeiling()).thenReturn(300);
        when(meetingRepository.findByIdWithLock(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pendingJoinRequestApprover.approveAll(meeting, hostId)).thenReturn(Result.success(1));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(AdmissionPolicy.ALLOW_ALL, 90, false, false, 30, false, "ALL", true, null),
                null));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(pendingJoinRequestApprover).approveAll(meeting, hostId);
    }

    @Test
    void execute_liveAllowGuestOpening_autoApprovesPendingRequests() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Meeting meeting = meetingWithStatus(
                meetingId,
                hostId,
                MeetingStatus.LIVE,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null));
        when(limitsConfig.getMaxParticipantsCeiling()).thenReturn(300);
        when(meetingRepository.findByIdWithLock(meetingId)).thenReturn(Optional.of(meeting));
        when(meetingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pendingJoinRequestApprover.approveAll(meeting, hostId)).thenReturn(Result.success(1));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        true,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null),
                null));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(pendingJoinRequestApprover).approveAll(meeting, hostId);
    }

    @Test
    void execute_liveAccessOpening_returnsFailureWhenAutoApprovalFails() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        Meeting meeting = meetingWithStatus(
                meetingId,
                hostId,
                MeetingStatus.LIVE,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null));
        when(limitsConfig.getMaxParticipantsCeiling()).thenReturn(300);
        when(meetingRepository.findByIdWithLock(meetingId)).thenReturn(Optional.of(meeting));
        when(pendingJoinRequestApprover.approveAll(meeting, hostId))
                .thenReturn(Result.failure(
                        new MeetingError.LiveKitUnavailable("token generation failed")));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(AdmissionPolicy.ALLOW_ALL, 90, false, false, 30, false, "ALL", true, null),
                null));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isEqualTo(new MeetingError.LiveKitUnavailable("token generation failed"));
        verify(meetingRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void execute_returnsInvalidStatusForCancelledMeeting() {
        UUID meetingId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        when(meetingRepository.findByIdWithLock(meetingId))
                .thenReturn(Optional.of(meetingWithStatus(
                        meetingId,
                        hostId,
                        MeetingStatus.CANCELLED,
                        settings(
                                AdmissionPolicy.MANUAL_APPROVAL,
                                90,
                                false,
                                false,
                                30,
                                false,
                                "ALL",
                                true,
                                null))));

        var result = useCase.execute(new PutMeetingSettingsCommand(
                meetingId,
                hostId,
                settings(
                        AdmissionPolicy.MANUAL_APPROVAL,
                        90,
                        false,
                        false,
                        30,
                        false,
                        "ALL",
                        true,
                        null),
                null));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isEqualTo(new MeetingError.InvalidStatusTransition(
                        MeetingStatus.CANCELLED, MeetingStatus.SCHEDULED));
        verifyNoInteractions(passwordHasher, pendingJoinRequestApprover, eventPublisher);
    }

    private static Meeting meetingWithStatus(
            UUID meetingId, UUID hostId, MeetingStatus status, MeetingSettings settings) {
        return Meeting.reconstitute(
                MeetingId.of(meetingId),
                UserId.of(hostId),
                ShortCode.of("ABC1234567"),
                null,
                null,
                null,
                null,
                MeetingType.INSTANT,
                status,
                settings,
                Instant.parse("2026-04-02T10:00:00Z"));
    }

    private static MeetingSettings settings(
            AdmissionPolicy admissionPolicy,
            Integer timeoutSeconds,
            boolean allowGuest,
            boolean muteOnEntry,
            int maxParticipants,
            boolean recordingEnabled,
            String screenShareMode,
            boolean chatEnabled,
            String passwordHash) {
        return new MeetingSettings(
                admissionPolicy,
                timeoutSeconds != null ? Duration.ofSeconds(timeoutSeconds) : null,
                allowGuest,
                muteOnEntry,
                maxParticipants,
                recordingEnabled,
                screenShareMode,
                chatEnabled,
                passwordHash);
    }
}
