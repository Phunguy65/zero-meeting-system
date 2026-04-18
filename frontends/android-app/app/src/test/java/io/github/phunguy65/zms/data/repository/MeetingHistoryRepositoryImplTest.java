package io.github.phunguy65.zms.data.repository;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.data.mapper.MeetingMapper;
import io.github.phunguy65.zms.data.remote.api.UserMeetingsApi;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementCursorScrollResponseMeetingResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingDetailResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingResponse;
import io.github.phunguy65.zms.domain.model.MeetingHistory;
import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.model.MeetingHistoryPage;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import retrofit2.Call;
import retrofit2.Response;

/** Unit tests for {@link MeetingHistoryRepositoryImpl}. */
@RunWith(MockitoJUnitRunner.class)
public class MeetingHistoryRepositoryImplTest {

    @Mock private UserMeetingsApi api;
    @Mock private MeetingMapper mapper;
    @Mock private Call<MeetingManagementCursorScrollResponseMeetingResponse> listCall;
    @Mock private Call<MeetingManagementMeetingDetailResponse> detailCall;

    private MeetingHistoryRepositoryImpl repository;

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String MEETING_ID = "22222222-2222-2222-2222-222222222222";

    @Before
    public void setup() {
        repository = new MeetingHistoryRepositoryImpl(api, mapper, Runnable::run);
    }

    private MeetingHistory domainItem() {
        OffsetDateTime now = OffsetDateTime.parse("2026-04-16T10:00:00Z");
        return new MeetingHistory(
                "id", "title", now, now.plusMinutes(30), MeetingType.SCHEDULED, MeetingStatus.ENDED);
    }

    // ------------------- getMeetingHistory -------------------

    @Test
    public void getMeetingHistory_success_mapsItemsAndSetsHasNextFromToken() throws Exception {
        MeetingManagementCursorScrollResponseMeetingResponse body =
                new MeetingManagementCursorScrollResponseMeetingResponse();
        MeetingManagementMeetingResponse dto = new MeetingManagementMeetingResponse();
        body.setContent(List.of(dto));
        body.setNextPageToken("next-token");

        when(api.listParticipatedMeetings(
                        eq(UUID.fromString(USER_ID)),
                        eq(20),
                        eq((String) null),
                        eq("ENDED,CANCELLED")))
                .thenReturn(listCall);
        when(listCall.execute()).thenReturn(Response.success(body));
        when(mapper.toMeetingHistory(dto)).thenReturn(domainItem());

        MeetingHistoryPage page = repository.getMeetingHistory(USER_ID, 20, null).get();

        assertEquals(1, page.items().size());
        assertEquals("next-token", page.nextPageToken());
        assertTrue(page.hasNext());
    }

    @Test
    public void getMeetingHistory_success_hasNextFalse_whenNoNextPageToken() throws Exception {
        MeetingManagementCursorScrollResponseMeetingResponse body =
                new MeetingManagementCursorScrollResponseMeetingResponse();
        body.setContent(List.of());
        body.setNextPageToken(null);

        when(api.listParticipatedMeetings(any(), anyInt(), any(), any())).thenReturn(listCall);
        when(listCall.execute()).thenReturn(Response.success(body));

        MeetingHistoryPage page = repository.getMeetingHistory(USER_ID, 20, null).get();

        assertEquals(0, page.items().size());
        assertNull(page.nextPageToken());
        assertFalse(page.hasNext());
    }

    @Test
    public void getMeetingHistory_httpError_failsFuture() {
        when(api.listParticipatedMeetings(any(), anyInt(), any(), any())).thenReturn(listCall);
        try {
            when(listCall.execute())
                    .thenReturn(
                            Response.error(500, ResponseBody.create(null, "err")));
        } catch (Exception ignored) {
        }

        try {
            repository.getMeetingHistory(USER_ID, 20, null).get();
            fail("Expected failure");
        } catch (ExecutionException | InterruptedException e) {
            assertTrue(e.getCause() instanceof CompletionException
                    || e.getCause() instanceof java.io.IOException);
        }
    }

    @Test
    public void getMeetingHistory_invalidUuid_failsFuture() {
        try {
            repository.getMeetingHistory("not-a-uuid", 20, null).get();
            fail("Expected failure");
        } catch (Exception e) {
            // expected — UUID.fromString throws
        }
    }

    // ------------------- getMeetingDetail -------------------

    @Test
    public void getMeetingDetail_success_mapsAndReturnsDomain() throws Exception {
        MeetingManagementMeetingDetailResponse body = new MeetingManagementMeetingDetailResponse();
        MeetingHistoryDetail domain =
                new MeetingHistoryDetail(
                        MEETING_ID,
                        "host",
                        "CODE",
                        "t",
                        null,
                        OffsetDateTime.parse("2026-04-16T10:00:00Z"),
                        null,
                        MeetingType.SCHEDULED,
                        MeetingStatus.ENDED,
                        OffsetDateTime.parse("2026-04-15T10:00:00Z"),
                        List.of(),
                        List.of());

        when(api.getParticipatedMeetingDetail(
                        eq(UUID.fromString(USER_ID)), eq(UUID.fromString(MEETING_ID))))
                .thenReturn(detailCall);
        when(detailCall.execute()).thenReturn(Response.success(body));
        when(mapper.toMeetingHistoryDetail(body)).thenReturn(domain);

        MeetingHistoryDetail result = repository.getMeetingDetail(USER_ID, MEETING_ID).get();

        assertSame(domain, result);
    }

    @Test
    public void getMeetingDetail_httpError_failsFuture() {
        when(api.getParticipatedMeetingDetail(any(), any())).thenReturn(detailCall);
        try {
            when(detailCall.execute())
                    .thenReturn(
                            Response.error(404, ResponseBody.create(null, "not found")));
        } catch (Exception ignored) {
        }

        try {
            repository.getMeetingDetail(USER_ID, MEETING_ID).get();
            fail("Expected failure");
        } catch (ExecutionException | InterruptedException e) {
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void getMeetingHistory_successfulResponseWithNullBody_failsFuture() throws Exception {
        when(api.listParticipatedMeetings(any(), anyInt(), any(), any())).thenReturn(listCall);
        when(listCall.execute()).thenReturn(Response.success(null));

        try {
            repository.getMeetingHistory(USER_ID, 20, null).get();
            fail("Expected failure");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof CompletionException);
        }
    }

    @Test
    public void getMeetingDetail_successfulResponseWithNullBody_failsFuture() throws Exception {
        when(api.getParticipatedMeetingDetail(any(), any())).thenReturn(detailCall);
        when(detailCall.execute()).thenReturn(Response.success(null));

        try {
            repository.getMeetingDetail(USER_ID, MEETING_ID).get();
            fail("Expected failure");
        } catch (ExecutionException e) {
            assertNotNull(e.getCause());
        }
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
