package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipatedMeetingDetailQuery;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.RecordingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTimeRange;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTitle;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingInviteeRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.RecordingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.InviteeSummary;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.ParticipantSummary;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.RecordingSummary;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetParticipatedMeetingDetailUseCaseTest {

    @Mock
    MeetingRepository meetingRepository;

    @Mock
    ParticipationLogRepository participationLogRepository;

    @Mock
    RecordingRepository recordingRepository;

    @Mock
    MeetingInviteeRepository meetingInviteeRepository;

    private GetParticipatedMeetingDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetParticipatedMeetingDetailUseCase(
                meetingRepository,
                participationLogRepository,
                recordingRepository,
                meetingInviteeRepository);
    }

    @Test
    void execute_rejectsRequesterOutsideUserScope() {
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        var result = useCase.execute(
                new GetParticipatedMeetingDetailQuery(userId, UUID.randomUUID(), requesterId));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isEqualTo(new MeetingError.NotOwner(requesterId, userId));
        verifyNoInteractions(
                participationLogRepository,
                meetingRepository,
                recordingRepository,
                meetingInviteeRepository);
    }

    @Test
    void execute_rejectsUserWithoutParticipation() {
        UUID userId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        when(participationLogRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(false);

        var result =
                useCase.execute(new GetParticipatedMeetingDetailQuery(userId, meetingId, userId));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isEqualTo(new MeetingError.NotParticipant(userId, meetingId));
        verify(participationLogRepository).existsByMeetingIdAndUserId(meetingId, userId);
        verifyNoInteractions(meetingRepository, recordingRepository, meetingInviteeRepository);
    }

    @Test
    void execute_returnsMeetingNotFoundWhenRepositoryEmpty() {
        UUID userId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        when(participationLogRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(true);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.empty());

        var result =
                useCase.execute(new GetParticipatedMeetingDetailQuery(userId, meetingId, userId));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, MeetingError>) result).error())
                .isEqualTo(new MeetingError.MeetingNotFound(meetingId));
    }

    @Test
    void execute_aggregatesMeetingParticipantsRecordingsAndInvitees() {
        UUID userId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        Meeting meeting = Meeting.reconstitute(
                MeetingId.of(meetingId),
                UserId.of(UUID.randomUUID()),
                ShortCode.of("ABC123"),
                MeetingTitle.of("Design Review"),
                "Discuss architecture",
                MeetingTimeRange.of(
                        Instant.parse("2026-04-01T10:00:00Z"),
                        Instant.parse("2026-04-01T11:00:00Z")),
                null,
                io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingType.SCHEDULED,
                MeetingStatus.ENDED,
                new MeetingSettings(
                        AdmissionPolicy.ALLOW_ALL,
                        null,
                        true,
                        false,
                        50,
                        true,
                        "HOST_ONLY",
                        true,
                        null),
                Instant.parse("2026-04-01T08:00:00Z"));
        when(participationLogRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(true);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(participationLogRepository.findDistinctParticipantSummariesByMeetingId(meetingId))
                .thenReturn(List.of(new ParticipantSummary(
                        1L,
                        meetingId,
                        userId,
                        "Alice",
                        ParticipantRole.PARTICIPANT.name(),
                        Instant.parse("2026-04-01T10:00:00Z"),
                        Instant.parse("2026-04-01T11:00:00Z"))));
        when(recordingRepository.findCompletedSummariesByMeetingId(meetingId))
                .thenReturn(List.of(new RecordingSummary(
                        UUID.randomUUID(),
                        meetingId,
                        "https://example.com/recording.mp4",
                        null,
                        RecordingStatus.COMPLETED,
                        Instant.parse("2026-04-01T10:00:00Z"),
                        Instant.parse("2026-04-01T11:00:00Z"),
                        3600,
                        1024L,
                        Instant.parse("2026-04-01T11:01:00Z"))));
        when(meetingInviteeRepository.findSummariesByMeetingId(meetingId))
                .thenReturn(List.of(new InviteeSummary(
                        userId,
                        "alice@example.com",
                        "Alice",
                        io.github.phunguy65.zms.meetingmanagement.domain.model.InviteeStatus
                                .ACCEPTED
                                .name(),
                        Instant.parse("2026-03-31T08:00:00Z"),
                        Instant.parse("2026-03-31T09:00:00Z"))));

        var result =
                useCase.execute(new GetParticipatedMeetingDetailQuery(userId, meetingId, userId));

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = ((Result.Success<
                                io.github.phunguy65.zms.meetingmanagement.application.response
                                        .MeetingDetailResponse,
                                MeetingError>)
                        result)
                .value();
        assertThat(response.id()).isEqualTo(meetingId);
        assertThat(response.participants()).singleElement().satisfies(participant -> {
            assertThat(participant.displayName()).isEqualTo("Alice");
            assertThat(participant.role()).isEqualTo(ParticipantRole.PARTICIPANT);
        });
        assertThat(response.recordings()).singleElement().satisfies(recording -> {
            assertThat(recording.status()).isEqualTo(RecordingStatus.COMPLETED);
            assertThat(recording.fileUrl()).isEqualTo("https://example.com/recording.mp4");
        });
        assertThat(response.invitees()).singleElement().satisfies(invitee -> {
            assertThat(invitee.email()).isEqualTo("alice@example.com");
            assertThat(invitee.displayName()).isEqualTo("Alice");
        });
    }

    @Test
    void execute_returnsEmptyNestedCollectionsWhenNoAggregatesExist() {
        UUID userId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        Meeting meeting = Meeting.reconstitute(
                MeetingId.of(meetingId),
                UserId.of(UUID.randomUUID()),
                ShortCode.of("XYZ789"),
                null,
                null,
                null,
                null,
                io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingType.INSTANT,
                MeetingStatus.LIVE,
                new MeetingSettings(
                        AdmissionPolicy.ALLOW_ALL,
                        null,
                        true,
                        false,
                        10,
                        false,
                        "HOST_ONLY",
                        true,
                        null),
                Instant.parse("2026-04-01T08:00:00Z"));
        when(participationLogRepository.existsByMeetingIdAndUserId(meetingId, userId))
                .thenReturn(true);
        when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(meeting));
        when(participationLogRepository.findDistinctParticipantSummariesByMeetingId(meetingId))
                .thenReturn(List.of());
        when(recordingRepository.findCompletedSummariesByMeetingId(meetingId))
                .thenReturn(List.of());
        when(meetingInviteeRepository.findSummariesByMeetingId(meetingId)).thenReturn(List.of());

        var result =
                useCase.execute(new GetParticipatedMeetingDetailQuery(userId, meetingId, userId));

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = ((Result.Success<
                                io.github.phunguy65.zms.meetingmanagement.application.response
                                        .MeetingDetailResponse,
                                MeetingError>)
                        result)
                .value();
        assertThat(response.title()).isNull();
        assertThat(response.description()).isNull();
        assertThat(response.startTime()).isNull();
        assertThat(response.endTime()).isNull();
        assertThat(response.participants()).isEmpty();
        assertThat(response.recordings()).isEmpty();
        assertThat(response.invitees()).isEmpty();
    }
}
