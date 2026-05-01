package io.github.phunguy65.zms.presentation.schedule;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import io.github.phunguy65.zms.domain.model.MeetingDetail;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import io.github.phunguy65.zms.domain.model.UpdateSettingsResult;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.meeting.CancelMeetingUseCase;
import io.github.phunguy65.zms.domain.usecase.meeting.GetInviteesUseCase;
import io.github.phunguy65.zms.domain.usecase.meeting.GetMeetingDetailUseCase;
import io.github.phunguy65.zms.domain.usecase.meeting.ResendInviteUseCase;
import io.github.phunguy65.zms.domain.usecase.meeting.RevokeInviteUseCase;
import io.github.phunguy65.zms.domain.usecase.meeting.ScheduleMeetingUseCase;
import io.github.phunguy65.zms.domain.usecase.meeting.UpdateMeetingSettingsUseCase;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link ScheduleViewModel}.
 * Covers cancel-meeting guard conditions and success/failure paths,
 * plus updateMeetingSettings resend-invite prompt flows.
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduleViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private ScheduleMeetingUseCase scheduleMeetingUseCase;

    @Mock
    private GetMeetingDetailUseCase getMeetingDetailUseCase;

    @Mock
    private UpdateMeetingSettingsUseCase updateMeetingSettingsUseCase;

    @Mock
    private CancelMeetingUseCase cancelMeetingUseCase;

    @Mock
    private GetInviteesUseCase getInviteesUseCase;

    @Mock
    private ResendInviteUseCase resendInviteUseCase;

    @Mock
    private RevokeInviteUseCase revokeInviteUseCase;

    @Mock
    private SessionRepository sessionRepository;

    private ScheduleViewModel viewModel;
    private final Executor immediateExecutor = Runnable::run;
    private static final String MEETING_ID = "meeting-uuid-123";

    @Before
    public void setup() {
        viewModel = new ScheduleViewModel(
                scheduleMeetingUseCase,
                getMeetingDetailUseCase,
                updateMeetingSettingsUseCase,
                cancelMeetingUseCase,
                getInviteesUseCase,
                resendInviteUseCase,
                revokeInviteUseCase,
                sessionRepository,
                immediateExecutor);
    }

    private MeetingDetail buildScheduledDetail() {
        MeetingSettings settings = new MeetingSettings.Builder()
                .requirePassword(false)
                .waitingRoomEnabled(false)
                .allowGuest(true)
                .maxParticipants(100)
                .build();

        return new MeetingDetail(
                MEETING_ID,
                "host-uuid",
                "ABC123",
                "Test Meeting",
                OffsetDateTime.now().plusHours(1),
                OffsetDateTime.now().plusHours(2),
                MeetingStatus.SCHEDULED,
                MeetingType.SCHEDULED,
                settings);
    }

    private MeetingDetail buildLiveDetail() {
        MeetingSettings settings = new MeetingSettings.Builder()
                .requirePassword(false)
                .waitingRoomEnabled(false)
                .allowGuest(true)
                .maxParticipants(100)
                .build();

        return new MeetingDetail(
                MEETING_ID,
                "host-uuid",
                "ABC123",
                "Test Meeting",
                OffsetDateTime.now().minusMinutes(30),
                OffsetDateTime.now().plusMinutes(30),
                MeetingStatus.LIVE,
                MeetingType.SCHEDULED,
                settings);
    }

    @Test
    public void cancelMeeting_withoutEditMode_isNoOp() {
        viewModel.cancelMeeting();

        verify(cancelMeetingUseCase, never()).execute(anyString());
        assertFalse(Boolean.TRUE.equals(viewModel.isCancelling.getValue()));
    }

    @Test
    public void cancelMeeting_withNullMeetingId_isNoOp() {
        viewModel.cancelMeeting();

        verify(cancelMeetingUseCase, never()).execute(anyString());
    }

    @Test
    public void cancelMeeting_whenMeetingStatusNotScheduled_isNoOp() {
        when(getMeetingDetailUseCase.execute(MEETING_ID))
                .thenReturn(CompletableFuture.completedFuture(buildLiveDetail()));
        viewModel.initEditMode(MEETING_ID);

        viewModel.cancelMeeting();

        verify(cancelMeetingUseCase, never()).execute(anyString());
    }

    @Test
    public void cancelMeeting_success_postsCancelSuccessAndClearsLoading() {
        when(getMeetingDetailUseCase.execute(MEETING_ID))
                .thenReturn(CompletableFuture.completedFuture(buildScheduledDetail()));
        viewModel.initEditMode(MEETING_ID);

        when(cancelMeetingUseCase.execute(MEETING_ID))
                .thenReturn(CompletableFuture.completedFuture(null));

        boolean[] successFired = {false};
        viewModel.cancelSuccess.observeForever(unused -> successFired[0] = true);

        viewModel.cancelMeeting();

        assertFalse(Boolean.TRUE.equals(viewModel.isCancelling.getValue()));
        assertTrue("cancelSuccess event should have fired", successFired[0]);
        verify(cancelMeetingUseCase).execute(MEETING_ID);
    }

    @Test
    public void cancelMeeting_failure_postsCancelErrorAndClearsLoading() {
        when(getMeetingDetailUseCase.execute(MEETING_ID))
                .thenReturn(CompletableFuture.completedFuture(buildScheduledDetail()));
        viewModel.initEditMode(MEETING_ID);

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Network error"));
        when(cancelMeetingUseCase.execute(MEETING_ID)).thenReturn(failedFuture);

        viewModel.cancelMeeting();

        assertFalse(Boolean.TRUE.equals(viewModel.isCancelling.getValue()));
        assertNotNull(viewModel.cancelError.getValue());
        assertTrue(viewModel.cancelError.getValue().contains("Network error"));
    }

    @Test
    public void
            updateMeetingSettings_whenResendRecommended_emitsInvalidatedCountViaResendInvitesPrompt() {
        when(getMeetingDetailUseCase.execute(MEETING_ID))
                .thenReturn(CompletableFuture.completedFuture(buildScheduledDetail()));
        viewModel.initEditMode(MEETING_ID);

        MeetingSettings updatedSettings = new MeetingSettings.Builder()
                .requirePassword(true)
                .waitingRoomEnabled(false)
                .allowGuest(true)
                .maxParticipants(100)
                .build();
        UpdateSettingsResult result = new UpdateSettingsResult(updatedSettings, 3, true);
        when(updateMeetingSettingsUseCase.execute(eq(MEETING_ID), any(MeetingSettings.class)))
                .thenReturn(CompletableFuture.completedFuture(result));

        int[] promptedCount = {-1};
        viewModel.resendInvitesPrompt.observeForever(count -> promptedCount[0] = count);

        viewModel.updateMeetingSettings(false, "new-password");

        assertFalse(Boolean.TRUE.equals(viewModel.isLoading.getValue()));
        assertEquals(3, promptedCount[0]);
        assertNotNull(viewModel.updateSuccess.getValue());
    }

    @Test
    public void updateMeetingSettings_whenResendNotRecommended_doesNotEmitResendInvitesPrompt() {
        when(getMeetingDetailUseCase.execute(MEETING_ID))
                .thenReturn(CompletableFuture.completedFuture(buildScheduledDetail()));
        viewModel.initEditMode(MEETING_ID);

        MeetingSettings updatedSettings = new MeetingSettings.Builder()
                .requirePassword(false)
                .waitingRoomEnabled(true)
                .allowGuest(true)
                .maxParticipants(100)
                .build();
        UpdateSettingsResult result = UpdateSettingsResult.noInvalidation(updatedSettings);
        when(updateMeetingSettingsUseCase.execute(eq(MEETING_ID), any(MeetingSettings.class)))
                .thenReturn(CompletableFuture.completedFuture(result));

        boolean[] promptFired = {false};
        viewModel.resendInvitesPrompt.observeForever(count -> promptFired[0] = true);

        viewModel.updateMeetingSettings(true, null);

        assertFalse(promptFired[0]);
        assertNotNull(viewModel.updateSuccess.getValue());
    }
}
