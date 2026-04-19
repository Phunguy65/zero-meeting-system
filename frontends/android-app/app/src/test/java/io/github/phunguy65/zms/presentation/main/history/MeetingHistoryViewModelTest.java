package io.github.phunguy65.zms.presentation.main.history;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import io.github.phunguy65.zms.domain.model.MeetingHistory;
import io.github.phunguy65.zms.domain.model.MeetingHistoryPage;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.history.GetMeetingHistoryUseCase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link MeetingHistoryViewModel}. */
@RunWith(MockitoJUnitRunner.class)
public class MeetingHistoryViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private GetMeetingHistoryUseCase useCase;

    @Mock
    private SessionRepository sessionRepository;

    private final Executor mainExecutor = Runnable::run;

    private static final String USER_ID = "user-123";

    private static MeetingHistory item(String id) {
        OffsetDateTime start = OffsetDateTime.parse("2026-04-16T10:00:00Z");
        return new MeetingHistory(
                id,
                "Meeting " + id,
                start,
                start.plusMinutes(30),
                MeetingType.SCHEDULED,
                MeetingStatus.ENDED);
    }

    @Before
    public void setup() {
        when(sessionRepository.getSession())
                .thenReturn(new SessionInfo(USER_ID, "e@x", "Full", "u", null));
    }

    @Test
    public void loadInitial_success_withItems_emitsSuccess() {
        when(useCase.execute(eq(USER_ID), eq(MeetingHistoryViewModel.PAGE_SIZE), isNull()))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("a")), "next", true)));

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);

        MeetingHistoryUiState state = vm.getState().getValue();
        assertTrue(state instanceof MeetingHistoryUiState.Success);
        MeetingHistoryUiState.Success success = (MeetingHistoryUiState.Success) state;
        assertEquals(1, success.items().size());
        assertEquals("next", success.nextPageToken());
        assertTrue(success.hasMore());
        assertFalse(success.isLoadingMore());
    }

    @Test
    public void loadInitial_success_withEmptyItems_emitsEmpty() {
        when(useCase.execute(any(), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(), null, false)));

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);

        assertTrue(vm.getState().getValue() instanceof MeetingHistoryUiState.Empty);
    }

    @Test
    public void loadInitial_failure_emitsError() {
        CompletableFuture<MeetingHistoryPage> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Network error"));
        when(useCase.execute(any(), anyInt(), any())).thenReturn(failed);

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);

        MeetingHistoryUiState state = vm.getState().getValue();
        assertTrue(state instanceof MeetingHistoryUiState.Error);
        assertEquals("Network error", ((MeetingHistoryUiState.Error) state).message());
    }

    @Test
    public void loadInitial_withNullSession_emitsError() {
        when(sessionRepository.getSession()).thenReturn(null);

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);

        assertTrue(vm.getState().getValue() instanceof MeetingHistoryUiState.Error);
        verifyNoInteractions(useCase);
    }

    @Test
    public void loadMore_appendsItemsAndUpdatesToken() {
        // Initial page
        when(useCase.execute(any(), anyInt(), isNull()))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("a")), "token-1", true)));
        // Next page
        when(useCase.execute(any(), anyInt(), eq("token-1")))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("b")), null, false)));

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);

        vm.loadMore();

        MeetingHistoryUiState state = vm.getState().getValue();
        assertTrue(state instanceof MeetingHistoryUiState.Success);
        MeetingHistoryUiState.Success success = (MeetingHistoryUiState.Success) state;
        assertEquals(2, success.items().size());
        assertNull(success.nextPageToken());
        assertFalse(success.hasMore());
    }

    @Test
    public void loadMore_whenNoMore_isNoOp() {
        when(useCase.execute(any(), anyInt(), isNull()))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("a")), null, false)));

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);
        reset(useCase);

        vm.loadMore();

        verifyNoInteractions(useCase);
    }

    @Test
    public void refresh_replacesItemsAndResetsToken() {
        when(useCase.execute(any(), anyInt(), isNull()))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("a")), "t1", true)))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("b"), item("c")), null, false)));

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);

        vm.refresh();

        MeetingHistoryUiState state = vm.getState().getValue();
        assertTrue(state instanceof MeetingHistoryUiState.Success);
        MeetingHistoryUiState.Success success = (MeetingHistoryUiState.Success) state;
        assertEquals(2, success.items().size());
        assertFalse(success.hasMore());
        assertFalse(success.isRefreshing());
    }

    @Test
    public void loadMore_failure_emitsPageErrorAndRestoresState() {
        when(useCase.execute(any(), anyInt(), isNull()))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("a")), "t1", true)));
        CompletableFuture<MeetingHistoryPage> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Boom"));
        when(useCase.execute(any(), anyInt(), eq("t1"))).thenReturn(failed);

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);

        vm.loadMore();

        assertEquals("Boom", vm.getPageErrorEvent().getValue());
        MeetingHistoryUiState state = vm.getState().getValue();
        assertTrue(state instanceof MeetingHistoryUiState.Success);
        // Items should remain intact after the error
        assertEquals(1, ((MeetingHistoryUiState.Success) state).items().size());
        assertFalse(((MeetingHistoryUiState.Success) state).isLoadingMore());
    }

    @Test
    public void refresh_failure_whileInSuccess_emitsPageErrorAndKeepsItems() {
        when(useCase.execute(any(), anyInt(), isNull()))
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("a")), "t1", true)));
        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);

        // Refresh fails
        CompletableFuture<MeetingHistoryPage> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Connection lost"));
        when(useCase.execute(any(), anyInt(), isNull())).thenReturn(failed);

        vm.refresh();

        assertTrue(vm.getState().getValue() instanceof MeetingHistoryUiState.Success);
        assertEquals("Connection lost", vm.getPageErrorEvent().getValue());
        MeetingHistoryUiState.Success s =
                (MeetingHistoryUiState.Success) vm.getState().getValue();
        assertEquals(1, s.items().size());
        assertFalse(s.isRefreshing());
    }

    @Test
    public void loadInitial_canBeRetriedAfterFailure() {
        CompletableFuture<MeetingHistoryPage> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("First attempt"));
        when(useCase.execute(any(), anyInt(), isNull()))
                .thenReturn(failed)
                .thenReturn(CompletableFuture.completedFuture(
                        new MeetingHistoryPage(List.of(item("a")), null, false)));

        MeetingHistoryViewModel vm =
                new MeetingHistoryViewModel(useCase, sessionRepository, mainExecutor);
        assertTrue(vm.getState().getValue() instanceof MeetingHistoryUiState.Error);

        vm.loadInitial();

        assertTrue(vm.getState().getValue() instanceof MeetingHistoryUiState.Success);
    }

    // Helper to provide Mockito's anyInt
    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
