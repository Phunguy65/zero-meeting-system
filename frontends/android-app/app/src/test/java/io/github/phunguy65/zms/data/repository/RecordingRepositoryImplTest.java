package io.github.phunguy65.zms.data.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.data.remote.api.RecordingsApi;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Unit tests for {@link RecordingRepositoryImpl}.
 * Covers start/stop success paths, HTTP error propagation, and invalid UUID handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class RecordingRepositoryImplTest {

    @Mock
    private RecordingsApi recordingsApi;

    @Mock
    private Call<io.github.phunguy65.zms.data.remote.dto.MeetingManagementRecordingResponse>
            startCall;

    @Mock
    private Call<Void> stopCall;

    private RecordingRepositoryImpl repository;
    private final Executor immediateExecutor = Runnable::run;

    private static final String VALID_UUID = "123e4567-e89b-12d3-a456-426614174000";

    @Before
    public void setup() {
        repository = new RecordingRepositoryImpl(recordingsApi, immediateExecutor);
    }

    @Test
    public void startRecording_success_completesWithoutException() throws IOException {
        UUID meetingUuid = UUID.fromString(VALID_UUID);
        Response<io.github.phunguy65.zms.data.remote.dto.MeetingManagementRecordingResponse>
                successResponse = Response.success(null);
        when(recordingsApi.startRecording(meetingUuid)).thenReturn(startCall);
        when(startCall.execute()).thenReturn(successResponse);

        CompletableFuture<Void> future = repository.startRecording(VALID_UUID);
        future.join();

        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    public void stopRecording_success_completesWithoutException() throws IOException {
        UUID meetingUuid = UUID.fromString(VALID_UUID);
        Response<Void> successResponse = Response.success(null);
        when(recordingsApi.stopRecording(meetingUuid)).thenReturn(stopCall);
        when(stopCall.execute()).thenReturn(successResponse);

        CompletableFuture<Void> future = repository.stopRecording(VALID_UUID);
        future.join();

        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    public void startRecording_httpError_propagatesAsCompletionException() throws IOException {
        UUID meetingUuid = UUID.fromString(VALID_UUID);
        Response<io.github.phunguy65.zms.data.remote.dto.MeetingManagementRecordingResponse>
                errorResponse = Response.error(409, ResponseBody.create(null, "{}"));
        when(recordingsApi.startRecording(meetingUuid)).thenReturn(startCall);
        when(startCall.execute()).thenReturn(errorResponse);

        CompletableFuture<Void> future = repository.startRecording(VALID_UUID);

        assertTrue(future.isCompletedExceptionally());
        try {
            future.join();
            fail("Expected CompletionException");
        } catch (CompletionException e) {
            assertTrue(e.getCause().getMessage().contains("409"));
        }
    }

    @Test
    public void stopRecording_httpError_propagatesAsCompletionException() throws IOException {
        UUID meetingUuid = UUID.fromString(VALID_UUID);
        Response<Void> errorResponse = Response.error(500, ResponseBody.create(null, "{}"));
        when(recordingsApi.stopRecording(meetingUuid)).thenReturn(stopCall);
        when(stopCall.execute()).thenReturn(errorResponse);

        CompletableFuture<Void> future = repository.stopRecording(VALID_UUID);

        assertTrue(future.isCompletedExceptionally());
        try {
            future.join();
            fail("Expected CompletionException");
        } catch (CompletionException e) {
            assertTrue(e.getCause().getMessage().contains("500"));
        }
    }

    @Test
    public void startRecording_invalidUuid_failsImmediatelyWithIllegalArgumentException() {
        CompletableFuture<Void> future = repository.startRecording("not-a-valid-uuid");

        assertTrue(future.isCompletedExceptionally());
        try {
            future.join();
            fail("Expected CompletionException");
        } catch (CompletionException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage().contains("Invalid meeting id"));
        }
        verifyNoInteractions(recordingsApi);
    }

    @Test
    public void stopRecording_invalidUuid_failsImmediatelyWithIllegalArgumentException() {
        CompletableFuture<Void> future = repository.stopRecording("not-a-valid-uuid");

        assertTrue(future.isCompletedExceptionally());
        try {
            future.join();
            fail("Expected CompletionException");
        } catch (CompletionException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            assertTrue(e.getCause().getMessage().contains("Invalid meeting id"));
        }
        verifyNoInteractions(recordingsApi);
    }
}
