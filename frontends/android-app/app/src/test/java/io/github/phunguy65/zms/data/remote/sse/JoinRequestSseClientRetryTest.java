package io.github.phunguy65.zms.data.remote.sse;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.os.Handler;
import android.os.Looper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.zms.domain.repository.JoinRoomRepository.ApprovalEventListener;
import java.lang.reflect.Field;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link JoinRequestSseClient} retry scheduling and terminal event suppression.
 *
 * <p>Mocks the Android {@link Looper} and {@link Handler} to verify retry behavior in a
 * pure JVM test environment without Robolectric.
 */
@RunWith(MockitoJUnitRunner.class)
public class JoinRequestSseClientRetryTest {

    @Mock
    OkHttpClient httpClient;

    @Mock
    OkHttpClient configuredClient;

    @Mock
    OkHttpClient.Builder clientBuilder;

    @Mock
    ObjectMapper objectMapper;

    @Mock
    ApprovalEventListener listener;

    @Mock
    Looper looper;

    private MockedStatic<Looper> looperStatic;
    private MockedConstruction<Handler> handlerConstruction;
    private Handler mockHandler;
    private JoinRequestSseClient sseClient;

    @Before
    public void setup() {
        looperStatic = mockStatic(Looper.class);
        looperStatic.when(Looper::getMainLooper).thenReturn(looper);

        when(httpClient.newBuilder()).thenReturn(clientBuilder);
        when(clientBuilder.readTimeout(anyLong(), any())).thenReturn(clientBuilder);
        when(clientBuilder.build()).thenReturn(configuredClient);

        handlerConstruction = mockConstruction(Handler.class, (mock, ctx) -> {
            mockHandler = mock;
            when(mock.post(any(Runnable.class))).thenAnswer(invocation -> {
                Runnable r = invocation.getArgument(0);
                r.run();
                return true;
            });
            when(mock.postDelayed(any(Runnable.class), anyLong())).thenReturn(true);
        });

        sseClient = new JoinRequestSseClient(httpClient, objectMapper);
    }

    @After
    public void teardown() {
        if (handlerConstruction != null) handlerConstruction.close();
        if (looperStatic != null) looperStatic.close();
    }

    @Test
    public void onFailure_schedulesRetryWithExponentialBackoff() throws Exception {
        setTerminated(false);
        setRetryCount(0);
        setCurrentListener(listener);
        setSavedFields("req-1", "token-1", listener);

        invokeScheduleRetry();

        verify(mockHandler).postDelayed(any(Runnable.class), eq(1000L));

        setRetryCount(1);
        invokeScheduleRetry();
        verify(mockHandler).postDelayed(any(Runnable.class), eq(2000L));

        setRetryCount(2);
        invokeScheduleRetry();
        verify(mockHandler).postDelayed(any(Runnable.class), eq(4000L));
    }

    @Test
    public void onFailure_noRetryAfterTerminalApprovedEvent() throws Exception {
        setTerminated(true);
        setRetryCount(0);
        setCurrentListener(listener);
        setSavedFields("req-1", "token-1", listener);

        invokeScheduleRetry();

        verify(mockHandler, never()).postDelayed(any(Runnable.class), anyLong());
    }

    @Test
    public void onFailure_noRetryAfterCancel() throws Exception {
        setTerminated(true);
        setRetryCount(0);
        setCurrentListener(listener);
        setSavedFields("req-1", "token-1", listener);

        invokeScheduleRetry();

        verify(mockHandler, never()).postDelayed(any(Runnable.class), anyLong());
    }

    @Test
    public void onFailure_callsOnErrorAfterMaxRetriesExhausted() throws Exception {
        setTerminated(false);
        setRetryCount(3);
        setCurrentListener(listener);

        invokeScheduleRetry();

        verify(mockHandler, never()).postDelayed(any(Runnable.class), anyLong());
        verify(listener).onError(contains("3 retries"));
    }

    @Test
    public void retryCount_incrementsOnEachRetryAttempt() throws Exception {
        setTerminated(false);
        setRetryCount(0);
        setCurrentListener(listener);
        setSavedFields("req-1", "token-1", listener);

        assertFieldValue("retryCount", 0);

        invokeScheduleRetry();
        assertFieldValue("retryCount", 1);

        invokeScheduleRetry();
        assertFieldValue("retryCount", 2);

        invokeScheduleRetry();
        assertFieldValue("retryCount", 3);
    }

    @Test
    public void cancel_setsTerminatedTrue() throws Exception {
        setTerminated(false);
        setCurrentListener(listener);

        sseClient.cancel();

        assertFieldValue("terminated", true);
    }

    private void setTerminated(boolean value) throws Exception {
        Field f = JoinRequestSseClient.class.getDeclaredField("terminated");
        f.setAccessible(true);
        f.set(sseClient, value);
    }

    private void setRetryCount(int count) throws Exception {
        Field f = JoinRequestSseClient.class.getDeclaredField("retryCount");
        f.setAccessible(true);
        f.set(sseClient, count);
    }

    private void setCurrentListener(ApprovalEventListener listener) throws Exception {
        Field f = JoinRequestSseClient.class.getDeclaredField("currentListener");
        f.setAccessible(true);
        f.set(sseClient, listener);
    }

    private void setSavedFields(String requestId, String authToken, ApprovalEventListener listener)
            throws Exception {
        Field fReqId = JoinRequestSseClient.class.getDeclaredField("savedRequestId");
        fReqId.setAccessible(true);
        fReqId.set(sseClient, requestId);

        Field fAuth = JoinRequestSseClient.class.getDeclaredField("savedAuthToken");
        fAuth.setAccessible(true);
        fAuth.set(sseClient, authToken);

        Field fListener = JoinRequestSseClient.class.getDeclaredField("savedListener");
        fListener.setAccessible(true);
        fListener.set(sseClient, listener);
    }

    private void invokeScheduleRetry() throws Exception {
        java.lang.reflect.Method m = JoinRequestSseClient.class.getDeclaredMethod("scheduleRetry");
        m.setAccessible(true);
        m.invoke(sseClient);
    }

    @SuppressWarnings("unchecked")
    private <T> void assertFieldValue(String fieldName, T expected) throws Exception {
        Field f = JoinRequestSseClient.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        assertEquals(expected, f.get(sseClient));
    }
}
