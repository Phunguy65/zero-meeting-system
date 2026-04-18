package io.github.phunguy65.zms.domain.usecase.history;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import io.github.phunguy65.zms.domain.repository.MeetingHistoryRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link GetMeetingDetailUseCase}. */
@RunWith(MockitoJUnitRunner.class)
public class GetMeetingDetailUseCaseTest {

    @Mock private MeetingHistoryRepository repository;

    private GetMeetingDetailUseCase useCase;

    private static final String USER_ID = "user-123";
    private static final String MEETING_ID = "meeting-xyz";

    @Before
    public void setup() {
        useCase = new GetMeetingDetailUseCase(repository);
    }

    private MeetingHistoryDetail detail() {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-16T10:00:00Z");
        return new MeetingHistoryDetail(
                MEETING_ID,
                "host",
                "CODE",
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
    public void execute_delegatesToRepositoryWithSameArgs() throws Exception {
        MeetingHistoryDetail d = detail();
        when(repository.getMeetingDetail(eq(USER_ID), eq(MEETING_ID)))
                .thenReturn(CompletableFuture.completedFuture(d));

        MeetingHistoryDetail result = useCase.execute(USER_ID, MEETING_ID).get();

        assertSame(d, result);
        verify(repository).getMeetingDetail(USER_ID, MEETING_ID);
    }

    @Test
    public void execute_propagatesRepositoryFailure() {
        CompletableFuture<MeetingHistoryDetail> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("Not found"));
        when(repository.getMeetingDetail(any(), any())).thenReturn(failed);

        CompletableFuture<MeetingHistoryDetail> result = useCase.execute(USER_ID, MEETING_ID);

        assertTrue(result.isCompletedExceptionally());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
