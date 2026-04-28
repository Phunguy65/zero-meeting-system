package io.github.phunguy65.zms.presentation.videocall;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import io.github.phunguy65.zms.data.repository.ChatDataMessageHandler;
import io.github.phunguy65.zms.domain.model.JoinRoomResult;
import io.github.phunguy65.zms.domain.model.MeetingDetail;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import io.github.phunguy65.zms.domain.repository.JoinRoomRepository;
import io.github.phunguy65.zms.domain.repository.LiveKitRepository;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import io.github.phunguy65.zms.domain.repository.ParticipantRepository;
import io.github.phunguy65.zms.domain.repository.RecordingRepository;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.repository.WaitingRoomRepository;
import io.github.phunguy65.zms.domain.usecase.meeting.EndMeetingUseCase;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link CallViewModel}.
 * Covers protected join flow: lookup success/failure, password-required state transitions,
 * password-aware join submission, and metadata-driven recording state transitions.
 */
@RunWith(MockitoJUnitRunner.class)
public class CallViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private LiveKitRepository liveKitRepository;

    @Mock
    private JoinRoomRepository joinRoomRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private WaitingRoomRepository waitingRoomRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private RecordingRepository recordingRepository;

    @Mock
    private EndMeetingUseCase endMeetingUseCase;

    @Mock
    private ChatDataMessageHandler chatDataMessageHandler;

    private CallViewModel viewModel;
    private LiveKitRepository.RoomEventListener capturedRoomEventListener;
    private final Executor immediateExecutor = Runnable::run;
    private static final String LIVEKIT_URL = "wss://test.livekit.cloud";

    @Before
    public void setup() {
        viewModel = new CallViewModel(
                liveKitRepository,
                joinRoomRepository,
                chatDataMessageHandler,
                sessionRepository,
                meetingRepository,
                waitingRoomRepository,
                participantRepository,
                recordingRepository,
                endMeetingUseCase,
                LIVEKIT_URL,
                immediateExecutor);

        ArgumentCaptor<LiveKitRepository.RoomEventListener> listenerCaptor =
                ArgumentCaptor.forClass(LiveKitRepository.RoomEventListener.class);
        verify(liveKitRepository).setRoomEventListener(listenerCaptor.capture());
        capturedRoomEventListener = listenerCaptor.getValue();
    }

    private MeetingDetail createMeetingDetail(boolean requirePassword) {
        MeetingSettings settings = new MeetingSettings.Builder()
                .requirePassword(requirePassword)
                .waitingRoomEnabled(false)
                .allowGuest(true)
                .maxParticipants(100)
                .build();

        return new MeetingDetail(
                "meeting-uuid-123",
                "host-uuid",
                "ABC123",
                "Test Meeting",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                MeetingStatus.SCHEDULED,
                MeetingType.SCHEDULED,
                settings);
    }

    @Test
    public void fetchMeetingInfoAndJoin_emptyShortCode_setsFetchError() {
        viewModel.fetchMeetingInfoAndJoin("");

        assertEquals("Meeting code is required", viewModel.getFetchError().getValue());
        assertFalse(viewModel.isFetchingMeetingInfo().getValue());
    }

    @Test
    public void fetchMeetingInfoAndJoin_nullShortCode_setsFetchError() {
        viewModel.fetchMeetingInfoAndJoin(null);

        assertEquals("Meeting code is required", viewModel.getFetchError().getValue());
    }

    @Test
    public void fetchMeetingInfoAndJoin_success_unprotectedMeeting_proceedsToJoin() {
        MeetingDetail detail = createMeetingDetail(false);
        when(meetingRepository.getMeetingByShortCode("ABC123"))
                .thenReturn(CompletableFuture.completedFuture(detail));

        viewModel.setMeetingCode("ABC123");
        viewModel.setDisplayName("Test User");
        viewModel.fetchMeetingInfoAndJoin("ABC123");

        assertFalse(viewModel.requiresPassword().getValue());
        assertTrue(viewModel.isReadyToJoin().getValue());
        assertFalse(viewModel.isFetchingMeetingInfo().getValue());
    }

    @Test
    public void fetchMeetingInfoAndJoin_success_protectedMeeting_revealsPasswordState() {
        MeetingDetail detail = createMeetingDetail(true);
        when(meetingRepository.getMeetingByShortCode("ABC123"))
                .thenReturn(CompletableFuture.completedFuture(detail));

        viewModel.setMeetingCode("ABC123");
        viewModel.fetchMeetingInfoAndJoin("ABC123");

        assertTrue(viewModel.requiresPassword().getValue());
        assertFalse(viewModel.isFetchingMeetingInfo().getValue());
        verify(joinRoomRepository, never()).requestJoin(any(), any(), any(), any(), any());
    }

    @Test
    public void fetchMeetingInfoAndJoin_failure_setsFetchError() {
        CompletableFuture<MeetingDetail> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Meeting not found"));
        when(meetingRepository.getMeetingByShortCode("INVALID")).thenReturn(failedFuture);

        viewModel.fetchMeetingInfoAndJoin("INVALID");

        assertNotNull(viewModel.getFetchError().getValue());
        assertTrue(viewModel.getFetchError().getValue().contains("not found"));
        assertFalse(viewModel.isFetchingMeetingInfo().getValue());
    }

    @Test
    public void fetchMeetingInfoAndJoin_setsLoadingState() {
        CompletableFuture<MeetingDetail> pendingFuture = new CompletableFuture<>();
        when(meetingRepository.getMeetingByShortCode("ABC123")).thenReturn(pendingFuture);

        viewModel.fetchMeetingInfoAndJoin("ABC123");

        assertTrue(viewModel.isFetchingMeetingInfo().getValue());
    }

    @Test
    public void setMeetingCode_differentCode_clearsPasswordState() {
        viewModel.setMeetingCode("ABC123");
        viewModel.setPassword("secret");

        MeetingDetail detail = createMeetingDetail(true);
        when(meetingRepository.getMeetingByShortCode("ABC123"))
                .thenReturn(CompletableFuture.completedFuture(detail));
        viewModel.fetchMeetingInfoAndJoin("ABC123");

        assertTrue(viewModel.requiresPassword().getValue());

        viewModel.setMeetingCode("XYZ789");

        assertFalse(viewModel.requiresPassword().getValue());
        assertEquals("", viewModel.getPassword().getValue());
    }

    @Test
    public void resetJoinState_clearsPasswordState() {
        viewModel.setPassword("secret");

        MeetingDetail detail = createMeetingDetail(true);
        when(meetingRepository.getMeetingByShortCode("ABC123"))
                .thenReturn(CompletableFuture.completedFuture(detail));
        viewModel.setMeetingCode("ABC123");
        viewModel.fetchMeetingInfoAndJoin("ABC123");

        viewModel.resetJoinState();

        assertFalse(viewModel.requiresPassword().getValue());
        assertEquals("", viewModel.getPassword().getValue());
        assertNull(viewModel.getFetchError().getValue());
        assertEquals(CallViewModel.JoinState.IDLE, viewModel.getJoinState().getValue());
    }

    @Test
    public void requestJoinRoom_withPassword_passesPasswordToRepository() {
        viewModel.setMeetingCode("ABC123");
        viewModel.setDisplayName("Test User");
        viewModel.setPassword("secret123");
        viewModel.setDeviceId("device-id");

        JoinRoomResult joinResult = JoinRoomResult.approved("token123", "meeting-uuid");
        when(joinRoomRepository.requestJoin(
                        anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(joinResult));

        viewModel.requestJoinRoom();

        verify(joinRoomRepository)
                .requestJoin(
                        eq("ABC123"), any(), eq("Test User"), eq("device-id"), eq("secret123"));
    }

    @Test
    public void requestJoinRoom_emptyPassword_passesNullToRepository() {
        viewModel.setMeetingCode("ABC123");
        viewModel.setDisplayName("Test User");
        viewModel.setPassword("");
        viewModel.setDeviceId("device-id");

        JoinRoomResult joinResult = JoinRoomResult.approved("token123", "meeting-uuid");
        when(joinRoomRepository.requestJoin(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(joinResult));

        viewModel.requestJoinRoom();

        verify(joinRoomRepository)
                .requestJoin(eq("ABC123"), any(), eq("Test User"), eq("device-id"), eq(null));
    }

    @Test
    public void setPassword_updatesPasswordState() {
        viewModel.setPassword("mypassword");

        assertEquals("mypassword", viewModel.getPassword().getValue());
    }

    @Test
    public void requestJoinRoom_invalidPassword_setsDeniedWithInvalidPasswordCode() {
        viewModel.setMeetingCode("ABC123");
        viewModel.setDisplayName("Test User");
        viewModel.setPassword("wrong-password");
        viewModel.setDeviceId("device-id");

        JoinRoomResult denyResult =
                JoinRoomResult.denied(JoinRoomResult.DenyReasonCode.INVALID_PASSWORD);
        when(joinRoomRepository.requestJoin(
                        anyString(), any(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(denyResult));

        viewModel.requestJoinRoom();

        assertEquals(CallViewModel.JoinState.DENIED, viewModel.getJoinState().getValue());
        assertEquals(
                JoinRoomResult.DenyReasonCode.INVALID_PASSWORD,
                viewModel.getDenyReasonCode().getValue());
    }

    @Test
    public void startRecording_success_clearsLoadingState() {
        viewModel.setMeetingId("meeting-uuid-123");

        when(recordingRepository.startRecording("meeting-uuid-123"))
                .thenReturn(CompletableFuture.completedFuture(null));

        viewModel.startRecording();

        assertFalse(viewModel.isRecordingLoading().getValue());
        assertNull(viewModel.getRecordingError().getValue());
    }

    @Test
    public void startRecording_failure_setsRecordingError() {
        viewModel.setMeetingId("meeting-uuid-123");

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
                new RuntimeException("Start recording failed: HTTP 409"));
        when(recordingRepository.startRecording("meeting-uuid-123")).thenReturn(failedFuture);

        viewModel.startRecording();

        assertFalse(viewModel.isRecordingLoading().getValue());
        assertNotNull(viewModel.getRecordingError().getValue());
        assertTrue(viewModel.getRecordingError().getValue().contains("409"));
    }

    @Test
    public void stopRecording_success_clearsLoadingState() {
        viewModel.setMeetingId("meeting-uuid-123");

        when(recordingRepository.stopRecording("meeting-uuid-123"))
                .thenReturn(CompletableFuture.completedFuture(null));

        viewModel.stopRecording();

        assertFalse(viewModel.isRecordingLoading().getValue());
        assertNull(viewModel.getRecordingError().getValue());
    }

    @Test
    public void stopRecording_failure_setsRecordingError() {
        viewModel.setMeetingId("meeting-uuid-123");

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Stop recording failed: HTTP 500"));
        when(recordingRepository.stopRecording("meeting-uuid-123")).thenReturn(failedFuture);

        viewModel.stopRecording();

        assertFalse(viewModel.isRecordingLoading().getValue());
        assertNotNull(viewModel.getRecordingError().getValue());
        assertTrue(viewModel.getRecordingError().getValue().contains("500"));
    }

    @Test
    public void startRecording_noMeetingId_doesNotCallRepository() {
        viewModel.startRecording();

        verify(recordingRepository, never()).startRecording(anyString());
    }

    @Test
    public void startRecording_whileLoading_doesNotCallRepositoryAgain() {
        viewModel.setMeetingId("meeting-uuid-123");

        CompletableFuture<Void> pendingFuture = new CompletableFuture<>();
        when(recordingRepository.startRecording("meeting-uuid-123")).thenReturn(pendingFuture);

        viewModel.startRecording();
        assertTrue(viewModel.isRecordingLoading().getValue());

        viewModel.startRecording();

        verify(recordingRepository, times(1)).startRecording("meeting-uuid-123");
    }

    @Test
    public void toggleRecording_whenNotRecording_callsStart() {
        viewModel.setMeetingId("meeting-uuid-123");

        when(recordingRepository.startRecording("meeting-uuid-123"))
                .thenReturn(CompletableFuture.completedFuture(null));

        viewModel.toggleRecording();

        verify(recordingRepository).startRecording("meeting-uuid-123");
        verify(recordingRepository, never()).stopRecording(anyString());
    }

    @Test
    public void toggleRecording_whenAlreadyRecording_callsStop() {
        viewModel.setMeetingId("meeting-uuid-123");
        capturedRoomEventListener.onRoomMetadataChanged("{\"recording\":true}");

        when(recordingRepository.stopRecording("meeting-uuid-123"))
                .thenReturn(CompletableFuture.completedFuture(null));

        viewModel.toggleRecording();

        verify(recordingRepository).stopRecording("meeting-uuid-123");
        verify(recordingRepository, never()).startRecording(anyString());
    }

    @Test
    public void roomMetadata_recordingTrue_setsIsRecordingTrue() {
        capturedRoomEventListener.onRoomMetadataChanged("{\"recording\":true}");

        assertTrue(viewModel.isRecording().getValue());
    }

    @Test
    public void roomMetadata_recordingFalse_clearsIsRecordingAndLoadingState() {
        capturedRoomEventListener.onRoomMetadataChanged("{\"recording\":true}");
        capturedRoomEventListener.onRoomMetadataChanged("{\"recording\":false}");

        assertFalse(viewModel.isRecording().getValue());
        assertFalse(viewModel.isRecordingLoading().getValue());
    }

    @Test
    public void roomMetadata_malformed_treatsRecordingAsInactive() {
        capturedRoomEventListener.onRoomMetadataChanged("not-valid-json{{");

        assertFalse(viewModel.isRecording().getValue());
    }

    @Test
    public void roomMetadata_empty_treatsRecordingAsInactive() {
        capturedRoomEventListener.onRoomMetadataChanged("");

        assertFalse(viewModel.isRecording().getValue());
    }

    @Test
    public void roomMetadata_null_treatsRecordingAsInactive() {
        capturedRoomEventListener.onRoomMetadataChanged(null);

        assertFalse(viewModel.isRecording().getValue());
    }

    @Test
    public void endMeetingForAll_noMeetingId_setsErrorAndDoesNotCallUseCase() {
        viewModel.endMeetingForAll();

        assertEquals(CallViewModel.ERROR_END_MEETING_NO_ID, viewModel.getEndMeetingError().getValue());
        assertFalse(viewModel.isEndingMeeting().getValue());
        verify(endMeetingUseCase, never()).execute(anyString());
    }

    @Test
    public void endMeetingForAll_notHost_setsErrorAndDoesNotCallUseCase() {
        viewModel.setMeetingId("meeting-uuid-123");
        viewModel.setIsHost(false);

        viewModel.endMeetingForAll();

        assertEquals(CallViewModel.ERROR_END_MEETING_NOT_HOST, viewModel.getEndMeetingError().getValue());
        assertFalse(viewModel.isEndingMeeting().getValue());
        verify(endMeetingUseCase, never()).execute(anyString());
    }

    @Test
    public void endMeetingForAll_success_clearsLoadingAndSignalsEnded() {
        viewModel.setMeetingId("meeting-uuid-123");
        viewModel.setIsHost(true);

        when(endMeetingUseCase.execute("meeting-uuid-123"))
                .thenReturn(CompletableFuture.completedFuture(null));

        viewModel.endMeetingForAll();

        assertFalse(viewModel.isEndingMeeting().getValue());
        assertNull(viewModel.getEndMeetingError().getValue());
        assertTrue(viewModel.getMeetingEndedForAll().getValue());
    }

    @Test
    public void endMeetingForAll_failure_setsErrorCodeAndKeepsUserInCall() {
        viewModel.setMeetingId("meeting-uuid-123");
        viewModel.setIsHost(true);

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("HTTP 503"));
        when(endMeetingUseCase.execute("meeting-uuid-123")).thenReturn(failedFuture);

        viewModel.endMeetingForAll();

        assertFalse(viewModel.isEndingMeeting().getValue());
        assertEquals(CallViewModel.ERROR_END_MEETING_FAILED, viewModel.getEndMeetingError().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getMeetingEndedForAll().getValue()));
    }
}
