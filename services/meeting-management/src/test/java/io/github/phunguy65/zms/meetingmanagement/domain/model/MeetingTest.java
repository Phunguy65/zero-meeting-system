package io.github.phunguy65.zms.meetingmanagement.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingCancelledEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTimeRange;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTitle;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MeetingTest {

    @Test
    void cancelScheduledMeeting_registersCancelledEventWithInvitees() {
        Meeting meeting = scheduledMeeting();

        Result<Void, MeetingError> result = meeting.cancel(
                "Planning Session",
                "ABC1234567",
                Instant.parse("2026-04-03T10:00:00Z"),
                List.of(new MeetingCancelledEvent.InviteeInfo(
                        UUID.randomUUID(),
                        "alice@example.com",
                        "Alice",
                        "PENDING",
                        Instant.parse("2026-04-02T09:00:00Z"))));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CANCELLED);
        assertThat(meeting.getDomainEvents())
                .last()
                .isInstanceOfSatisfying(MeetingCancelledEvent.class, event -> {
                    assertThat(event.meetingTitle()).isEqualTo("Planning Session");
                    assertThat(event.meetingShortCode()).isEqualTo("ABC1234567");
                    assertThat(event.invitees()).hasSize(1);
                });
    }

    @Test
    void cancelScheduledMeeting_withoutInvitees_registersEmptyInviteeList() {
        Meeting meeting = scheduledMeeting();

        Result<Void, MeetingError> result = meeting.cancel();

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(meeting.getDomainEvents())
                .last()
                .isInstanceOfSatisfying(
                        MeetingCancelledEvent.class,
                        event -> assertThat(event.invitees()).isEmpty());
    }

    @Test
    void cancelLiveMeeting_fails() {
        Meeting meeting = scheduledMeeting();
        assertThat(meeting.start()).isInstanceOf(Result.Success.class);

        Result<Void, MeetingError> result = meeting.cancel();

        assertThat(result)
                .isInstanceOfSatisfying(Result.Failure.class, failure -> assertThat(failure.error())
                        .isEqualTo(new MeetingError.InvalidStatusTransition(
                                MeetingStatus.LIVE, MeetingStatus.CANCELLED)));
    }

    @Test
    void cancelEndedMeeting_fails() {
        Meeting meeting = scheduledMeeting();
        assertThat(meeting.start()).isInstanceOf(Result.Success.class);
        assertThat(meeting.end()).isInstanceOf(Result.Success.class);

        Result<Void, MeetingError> result = meeting.cancel();

        assertThat(result)
                .isInstanceOfSatisfying(Result.Failure.class, failure -> assertThat(failure.error())
                        .isEqualTo(new MeetingError.InvalidStatusTransition(
                                MeetingStatus.ENDED, MeetingStatus.CANCELLED)));
    }

    @Test
    void cancelCancelledMeeting_fails() {
        Meeting meeting = scheduledMeeting();
        assertThat(meeting.cancel()).isInstanceOf(Result.Success.class);

        Result<Void, MeetingError> result = meeting.cancel();

        assertThat(result)
                .isInstanceOfSatisfying(Result.Failure.class, failure -> assertThat(failure.error())
                        .isEqualTo(new MeetingError.InvalidStatusTransition(
                                MeetingStatus.CANCELLED, MeetingStatus.CANCELLED)));
    }

    @Test
    void cancelInstantMeeting_isAllowedWhileStillScheduled() {
        Meeting meeting = Meeting.instant(
                UserId.of(UUID.randomUUID()),
                MeetingTitle.of("Instant"),
                null,
                MeetingSettings.defaults(),
                ShortCode.of("ABC1234567"));

        Result<Void, MeetingError> result = meeting.cancel();

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.CANCELLED);
    }

    private static Meeting scheduledMeeting() {
        return Meeting.schedule(
                UserId.of(UUID.randomUUID()),
                MeetingTitle.of("Planning Session"),
                "Discuss roadmap",
                MeetingTimeRange.of(
                        Instant.parse("2026-04-03T10:00:00Z"),
                        Instant.parse("2026-04-03T11:00:00Z")),
                MeetingSettings.defaults(),
                ShortCode.of("ABC1234567"));
    }
}
