package io.github.phunguy65.zms.domain.usecase.history;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.domain.model.MeetingHistoryPage;
import io.github.phunguy65.zms.domain.repository.MeetingHistoryRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link GetMeetingHistoryUseCase}. */
@RunWith(MockitoJUnitRunner.class)
public class GetMeetingHistoryUseCaseTest {

    @Mock private MeetingHistoryRepository repository;

    private GetMeetingHistoryUseCase useCase;

    @Before
    public void setup() {
        useCase = new GetMeetingHistoryUseCase(repository);
    }

    @Test
    public void execute_delegatesToRepositoryWithSameArgs() throws Exception {
        MeetingHistoryPage page = new MeetingHistoryPage(List.of(), "token", true);
        when(repository.getMeetingHistory(eq("u"), eq(20), eq("tok")))
                .thenReturn(CompletableFuture.completedFuture(page));

        MeetingHistoryPage result = useCase.execute("u", 20, "tok").get();

        assertSame(page, result);
        verify(repository).getMeetingHistory("u", 20, "tok");
    }

    @Test
    public void execute_passesNullPageTokenForInitialLoad() throws Exception {
        MeetingHistoryPage page = new MeetingHistoryPage(List.of(), null, false);
        when(repository.getMeetingHistory(eq("u"), eq(20), eq((String) null)))
                .thenReturn(CompletableFuture.completedFuture(page));

        MeetingHistoryPage result = useCase.execute("u", 20, null).get();

        assertSame(page, result);
    }

    @Test
    public void execute_propagatesRepositoryFailure() {
        CompletableFuture<MeetingHistoryPage> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("nope"));
        when(repository.getMeetingHistory(any(), anyInt(), any())).thenReturn(failed);

        CompletableFuture<MeetingHistoryPage> result = useCase.execute("u", 20, null);

        assertTrue(result.isCompletedExceptionally());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
