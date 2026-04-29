package io.github.phunguy65.zms.presentation.main.history;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;
import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.history.GetMeetingDetailUseCase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link MeetingDetailViewModel}. */
@RunWith(MockitoJUnitRunner.class)
public class MeetingDetailViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private GetMeetingDetailUseCase useCase;

    @Mock
    private SessionRepository sessionRepository;

    private final Executor mainExecutor = Runnable::run;
    private static final String USER_ID = "user-123";
    private static final String MEETING_ID = "meeting-abc";

    private MeetingDetailViewModel newViewModel(String meetingId) {
        SavedStateHandle handle = new SavedStateHandle();
        if (meetingId != null) {
            handle.set(MeetingDetailViewModel.ARG_MEETING_ID, meetingId);
        }
        when(sessionRepository.getSession())
                .thenReturn(new SessionInfo(USER_ID, "e@x", "Full", "u", null));
        return new MeetingDetailViewModel(handle, useCase, sessionRepository, mainExecutor);
    }

    private static MeetingHistoryDetail buildDetail() {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-16T10:00:00Z");
        return new MeetingHistoryDetail(
                MEETING_ID,
                "host",
                "SHORT",
                "Title",
                null,
                now,
                now.plusMinutes(30),
                MeetingType.SCHEDULED,
                MeetingStatus.ENDED,
                now,
                List.of(),
                List.of());
    }

    @Test
    public void load_success_emitsSuccess() {
        when(useCase.execute(eq(USER_ID), eq(MEETING_ID)))
                .thenReturn(CompletableFuture.completedFuture(buildDetail()));

        MeetingDetailViewModel vm = newViewModel(MEETING_ID);

        MeetingDetailUiState state = vm.getState().getValue();
        assertTrue(state instanceof MeetingDetailUiState.Success);
        assertEquals(MEETING_ID, ((MeetingDetailUiState.Success) state).detail().id());
    }

    @Test
    public void load_failure_emitsError() {
        CompletableFuture<MeetingHistoryDetail> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Oops"));
        when(useCase.execute(any(), any())).thenReturn(failed);

        MeetingDetailViewModel vm = newViewModel(MEETING_ID);

        MeetingDetailUiState state = vm.getState().getValue();
        assertTrue(state instanceof MeetingDetailUiState.Error);
        assertEquals("Oops", ((MeetingDetailUiState.Error) state).message());
    }

    @Test
    public void load_missingMeetingId_emitsError() {
        MeetingDetailViewModel vm = newViewModel(null);

        MeetingDetailUiState state = vm.getState().getValue();
        assertTrue(state instanceof MeetingDetailUiState.Error);
        verifyNoInteractions(useCase);
    }

    @Test
    public void load_missingSession_emitsError() {
        SavedStateHandle handle = new SavedStateHandle();
        handle.set(MeetingDetailViewModel.ARG_MEETING_ID, MEETING_ID);
        when(sessionRepository.getSession()).thenReturn(null);

        MeetingDetailViewModel vm =
                new MeetingDetailViewModel(handle, useCase, sessionRepository, mainExecutor);

        assertTrue(vm.getState().getValue() instanceof MeetingDetailUiState.Error);
        verifyNoInteractions(useCase);
    }

    @Test
    public void load_canBeRetriedManually() {
        CompletableFuture<MeetingHistoryDetail> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Once"));
        when(useCase.execute(any(), any()))
                .thenReturn(failed)
                .thenReturn(CompletableFuture.completedFuture(buildDetail()));

        MeetingDetailViewModel vm = newViewModel(MEETING_ID);
        assertTrue(vm.getState().getValue() instanceof MeetingDetailUiState.Error);

        vm.load();

        assertTrue(vm.getState().getValue() instanceof MeetingDetailUiState.Success);
    }
}
