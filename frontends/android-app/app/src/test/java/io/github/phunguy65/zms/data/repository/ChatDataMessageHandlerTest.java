package io.github.phunguy65.zms.data.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import android.util.Log;
import io.github.phunguy65.zms.data.mapper.ChatMessageMapper;
import io.github.phunguy65.zms.domain.model.ChatMessage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link ChatDataMessageHandler}.
 * Validates graceful handling of null, empty, malformed, and non-JSON payloads.
 */
public class ChatDataMessageHandlerTest {

    @Mock
    private ChatMessageMapper mapper;

    @Mock
    private ChatRepositoryImpl chatRepository;

    private ChatDataMessageHandler handler;

    private MockedStatic<Log> logMock;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        logMock = mockStatic(Log.class);
        handler = new ChatDataMessageHandler(mapper, chatRepository);
    }

    @After
    public void tearDown() {
        logMock.close();
    }

    @Test
    public void handleDataReceived_nullData_doesNothing() {
        handler.handleDataReceived(null);

        verifyNoInteractions(mapper);
        verifyNoInteractions(chatRepository);
    }

    @Test
    public void handleDataReceived_emptyData_doesNothing() {
        handler.handleDataReceived(new byte[0]);

        verifyNoInteractions(mapper);
        verifyNoInteractions(chatRepository);
    }

    @Test
    public void handleDataReceived_nonJsonData_doesNotCrash() {
        byte[] data = "this is not json".getBytes(StandardCharsets.UTF_8);

        handler.handleDataReceived(data);

        verify(chatRepository, never()).dispatchIncomingMessage(any());
    }

    @Test
    public void handleDataReceived_malformedJson_doesNotCrash() {
        byte[] data = "{\"id\": \"abc\", broken}".getBytes(StandardCharsets.UTF_8);

        handler.handleDataReceived(data);

        verify(chatRepository, never()).dispatchIncomingMessage(any());
    }

    @Test
    public void handleDataReceived_emptyJsonObject_doesNotDispatch() {
        byte[] data = "{}".getBytes(StandardCharsets.UTF_8);

        handler.handleDataReceived(data);

        verify(chatRepository, never()).dispatchIncomingMessage(any());
    }

    @Test
    public void handleDataReceived_missingContentField_doesNotDispatch() {
        byte[] data = "{\"id\": \"m1\"}".getBytes(StandardCharsets.UTF_8);

        handler.handleDataReceived(data);

        verify(chatRepository, never()).dispatchIncomingMessage(any());
    }

    @Test
    public void handleDataReceived_missingIdField_doesNotDispatch() {
        byte[] data = "{\"content\": \"hello\"}".getBytes(StandardCharsets.UTF_8);

        handler.handleDataReceived(data);

        verify(chatRepository, never()).dispatchIncomingMessage(any());
    }

    @Test
    public void handleDataReceived_binaryGarbage_doesNotCrash() {
        byte[] data = new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x80};

        handler.handleDataReceived(data);

        verify(chatRepository, never()).dispatchIncomingMessage(any());
    }

    @Test
    public void handleDataReceived_validPayload_dispatchesMessage() {
        String json = "{\"id\":\"m1\",\"seqNum\":1,\"senderId\":\"u1\","
                + "\"senderName\":\"Alice\",\"content\":\"hello\","
                + "\"type\":\"TEXT\",\"createdAt\":\"2026-04-22T10:00:00+07:00\"}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        ChatMessage message = new ChatMessage(
                "m1",
                1,
                "u1",
                "Alice",
                "hello",
                ChatMessage.MessageType.TEXT,
                OffsetDateTime.now());
        when(mapper.fromLiveKitPayload(any())).thenReturn(message);

        handler.handleDataReceived(data);

        verify(chatRepository).dispatchIncomingMessage(message);
    }

    @Test
    public void handleDataReceived_mapperReturnsNull_doesNotDispatch() {
        String json = "{\"id\":\"m1\",\"content\":\"hello\"}";
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        when(mapper.fromLiveKitPayload(any())).thenReturn(null);

        handler.handleDataReceived(data);

        verify(chatRepository, never()).dispatchIncomingMessage(any());
    }
}
